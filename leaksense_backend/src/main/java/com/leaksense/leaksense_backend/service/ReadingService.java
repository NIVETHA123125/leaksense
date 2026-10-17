package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private static final double NORMAL_OXYGEN = 20.9;
    private static final List<String> SEVERITY_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final SensorNodeRepository nodeRepository;
    private final ZoneRepository zoneRepository;
    private final RiskScoreService riskScoreService;
    private final SettingsService settingsService;
    private final ThresholdEvaluator thresholdEvaluator;
    private final NotificationService notificationService;
    private final CommandService commandService;
    private final AlarmService alarmService;
    private final SensorHealthService sensorHealthService;
    private final SimpMessagingTemplate messagingTemplate;

    public Reading process(SensorNode node, double methane, double pressure,
                           double oxygen, Integer battery) {
        LocalDateTime now = LocalDateTime.now();
        ThresholdSetting limits = settingsService.get();
        ThresholdEvaluator.Evaluation evaluation =
                thresholdEvaluator.evaluate(methane, pressure, oxygen, limits);

        double usableMethane = isFault(evaluation.methane()) ? 0.0 : methane;
        double usablePressure = isFault(evaluation.pressure()) ? limits.getPressureNormalKpa() : pressure;
        double usableOxygen = isFault(evaluation.oxygen()) ? NORMAL_OXYGEN : oxygen;
        RiskScoreService.RiskResult risk =
                riskScoreService.calculate(usableMethane, usablePressure, usableOxygen);

        Reading saved = readingRepository.save(
                new Reading(null, methane, pressure, oxygen, risk.score(), now, node));
        messagingTemplate.convertAndSend("/topic/readings", saved);

        boolean wasOffline = "OFFLINE".equalsIgnoreCase(node.getStatus());
        node.setStatus("ONLINE");
        node.setLastSeen(now);
        if (battery != null) {
            node.setBatteryLevel(battery);
        }
        nodeRepository.save(node);

        if (wasOffline) {
            notificationService.sendNodeRestored(node);
        }

        for (Alert offlineAlert : alertRepository.findByNodeIdAndLevelAndStatusNot(
                node.getId(), "DEVICE", "RESOLVED")) {
            offlineAlert.setStatus("RESOLVED");
            offlineAlert.setResolvedAt(now);
            Alert resolvedAlert = alertRepository.save(offlineAlert);
            messagingTemplate.convertAndSend("/topic/alerts", resolvedAlert);
        }

        Zone zone = node.getZone();
        if (zone != null) {
            zone.setRiskLevel(risk.level());
            zoneRepository.save(zone);
        }

        for (ThresholdEvaluator.SensorCheck check : evaluation.all()) {
            handleCheck(node, zone, check, now);
        }
        alarmService.evaluate(node, evaluation, now);
        sensorHealthService.check(node, saved);

        return saved;
    }

    private boolean isFault(ThresholdEvaluator.SensorCheck check) {
        return check.status() == ThresholdEvaluator.Status.FAULT;
    }

    private void handleCheck(SensorNode node, Zone zone,
                             ThresholdEvaluator.SensorCheck check, LocalDateTime now) {
        if (check.status() == ThresholdEvaluator.Status.FAULT) {
            raiseOrEscalate(node, zone, check, "FAULT", "HIGH", now);
            return;
        }

        resolveFaults(node, check, now);

        if (check.status() == ThresholdEvaluator.Status.WARNING) {
            raiseOrEscalate(node, zone, check, "SENSOR", "MEDIUM", now);
        } else if (check.status() == ThresholdEvaluator.Status.CRITICAL) {
            raiseOrEscalate(node, zone, check, "SENSOR", "CRITICAL", now);
        }
    }

    private void resolveFaults(SensorNode node, ThresholdEvaluator.SensorCheck check,
                               LocalDateTime now) {
        for (Alert fault : alertRepository.findByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), check.sensor(), "FAULT", "RESOLVED")) {
            fault.setStatus("RESOLVED");
            fault.setResolvedAt(now);
            Alert resolved = alertRepository.save(fault);
            messagingTemplate.convertAndSend("/topic/alerts", resolved);
        }
    }

    private void raiseOrEscalate(SensorNode node, Zone zone, ThresholdEvaluator.SensorCheck check,
                                 String level, String severity, LocalDateTime now) {
        Optional<Alert> open = alertRepository.findFirstByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), check.sensor(), level, "RESOLVED");

        String title = titleFor(check, node, zone, level);
        String message = messageFor(check, node, zone, level);

        if (open.isEmpty()) {
            Alert alert = new Alert(null, title, message, level, severity, "OPEN",
                    null, now, null, zone, node, check.sensor());
            Alert created = alertRepository.save(alert);
            publish(created);
            autoCloseIfNeeded(created, zone);
            return;
        }

        Alert existing = open.get();
        if (rank(severity) > rank(existing.getSeverity())) {
            existing.setSeverity(severity);
            existing.setTitle(title);
            existing.setMessage(message);
            existing.setStatus("OPEN");
            existing.setAcknowledgedBy(null);
            Alert upgraded = alertRepository.save(existing);
            publish(upgraded);
            autoCloseIfNeeded(upgraded, zone);
        }
    }

    private void publish(Alert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
        notificationService.sendAlertEmail(alert);
    }

    private void autoCloseIfNeeded(Alert alert, Zone zone) {
        if ("SENSOR".equals(alert.getLevel()) && "CRITICAL".equals(alert.getSeverity())) {
            commandService.autoCloseZone(zone, alert);
        }
    }

    private int rank(String severity) {
        return SEVERITY_ORDER.indexOf(severity);
    }

    private String zoneName(Zone zone) {
        return zone == null ? "zone" : zone.getName();
    }

    private String titleFor(ThresholdEvaluator.SensorCheck check, SensorNode node,
                            Zone zone, String level) {
        if ("FAULT".equals(level)) {
            return check.label() + " sensor fault on " + node.getNodeCode();
        }
        String direction = "HIGH".equals(check.direction()) ? "High " : "Low ";
        return direction + check.label().toLowerCase() + " in " + zoneName(zone);
    }

    private String messageFor(ThresholdEvaluator.SensorCheck check, SensorNode node,
                              Zone zone, String level) {
        String side = "HIGH".equals(check.direction()) ? "above" : "below";
        String reading = String.format("%.1f %s", check.value(), check.unit());
        String limit = String.format("%.1f %s", check.limit(), check.unit());

        if ("FAULT".equals(level)) {
            return check.label() + " reading of " + reading + " is " + side
                    + " the valid sensor range limit of " + limit + " on " + node.getNodeCode()
                    + ". The sensor may be faulty.";
        }
        return check.label() + " reached " + reading + ", " + side + " the "
                + check.status().name().toLowerCase() + " limit of " + limit
                + " on " + node.getNodeCode() + " (" + zoneName(zone) + ").";
    }
}