package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorHealthService {

    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${leaksense.sensor.stuck-count:5}")
    private int stuckCount;

    @Value("${leaksense.sensor.spike.methane-percent:100}")
    private double methaneSpikePct;

    @Value("${leaksense.sensor.spike.pressure-percent:15}")
    private double pressureSpikePct;

    @Value("${leaksense.sensor.spike.oxygen-percent:10}")
    private double oxygenSpikePct;

    /**
     * Run all sensor-health checks after a new reading is saved.
     * Called from ReadingService.process().
     */
    public void check(SensorNode node, Reading current) {
        checkSpike(node, current);
        checkStuck(node);
    }

    // ── Spike detection ──────────────────────────────────────────────

    void checkSpike(SensorNode node, Reading current) {
        Reading previous = readingRepository
                .findFirstByNodeIdOrderByRecordedAtDesc(node.getId())
                .orElse(null);

        if (previous == null || previous.getId().equals(current.getId())) {
            return;
        }

        checkOneSpike(node, "METHANE", "Methane", "ppm",
                previous.getMethanePpm(), current.getMethanePpm(), methaneSpikePct);
        checkOneSpike(node, "PRESSURE", "Pressure", "kPa",
                previous.getPressureKpa(), current.getPressureKpa(), pressureSpikePct);
        checkOneSpike(node, "OXYGEN", "Oxygen", "%",
                previous.getOxygenPercent(), current.getOxygenPercent(), oxygenSpikePct);
    }

    private void checkOneSpike(SensorNode node, String sensor, String label, String unit,
                                double previous, double current, double thresholdPct) {
        if (previous == 0.0) {
            return;
        }

        double changePct = Math.abs(current - previous) / Math.abs(previous) * 100.0;

        if (changePct <= thresholdPct) {
            return;
        }

        if (alertAlreadyOpen(node, sensor, "SENSOR") || alertAlreadyOpen(node, sensor, "FAULT")) {
            return;
        }

        String title = label + " spike on " + node.getNodeCode();
        String message = String.format(Locale.ROOT,
                "%s jumped from %.1f to %.1f %s (%.0f%% change) on %s. Possible sensor malfunction.",
                label, previous, current, unit, changePct, node.getNodeCode());

        raiseAlert(node, title, message, "SENSOR", "HIGH", sensor);
    }

    // ── Stuck sensor detection ───────────────────────────────────────

    void checkStuck(SensorNode node) {
        List<Reading> recent = readingRepository.findByNodeIdOrderByRecordedAtDesc(
                node.getId(), PageRequest.of(0, stuckCount));

        if (recent.size() < stuckCount) {
            return;
        }

        checkOneStuck(node, "METHANE", "Methane", "ppm",
                recent.stream().mapToDouble(Reading::getMethanePpm).toArray());
        checkOneStuck(node, "PRESSURE", "Pressure", "kPa",
                recent.stream().mapToDouble(Reading::getPressureKpa).toArray());
        checkOneStuck(node, "OXYGEN", "Oxygen", "%",
                recent.stream().mapToDouble(Reading::getOxygenPercent).toArray());
    }

    private void checkOneStuck(SensorNode node, String sensor, String label, String unit,
                                double[] values) {
        double first = values[0];
        for (int i = 1; i < values.length; i++) {
            if (Double.compare(values[i], first) != 0) {
                return;
            }
        }

        if (alertAlreadyOpen(node, sensor, "FAULT")) {
            return;
        }

        String title = label + " sensor may be stuck on " + node.getNodeCode();
        String message = String.format(Locale.ROOT,
                "%s sensor reported %.1f %s for the last %d consecutive readings on %s. "
                        + "The sensor may be faulty.",
                label, first, unit, stuckCount, node.getNodeCode());

        raiseAlert(node, title, message, "FAULT", "MEDIUM", sensor);
    }

    // ── helpers ──────────────────────────────────────────────────────

    private boolean alertAlreadyOpen(SensorNode node, String sensor, String level) {
        return alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), sensor, level, "RESOLVED");
    }

    private void raiseAlert(SensorNode node, String title, String message,
                            String level, String severity, String sensor) {
        Zone zone = node.getZone();
        LocalDateTime now = LocalDateTime.now();

        Alert alert = new Alert(null, title, message, level, severity, "OPEN",
                null, now, null, zone, node, sensor);

        Alert saved = alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", saved);
        notificationService.sendAlertEmail(saved);
        log.info("Sensor health alert: {}", title);
    }
}
