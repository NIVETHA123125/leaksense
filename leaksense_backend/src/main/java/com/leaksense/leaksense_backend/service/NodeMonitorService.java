package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NodeMonitorService {

    static final Set<String> VALVE_MOVING = Set.of("CLOSING", "OPENING");

    private final SensorNodeRepository nodeRepository;
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Value("${leaksense.node.offline-after-seconds:180}")
    private long offlineAfterSeconds;

    @Value("${leaksense.node.low-battery-percent:20}")
    private int lowBatteryPercent;

    @Value("${leaksense.node.critical-battery-percent:10}")
    private int criticalBatteryPercent;

    @Value("${leaksense.node.valve-stuck-seconds:120}")
    private long valveStuckSeconds;

    @Scheduled(fixedRate = 30000)
    public void checkNodes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(offlineAfterSeconds);

        for (SensorNode node : nodeRepository.findAll()) {
            checkOffline(node, cutoff, now);
            checkBattery(node, now);
            checkValveStuck(node, now);
        }
    }

    // ── Offline detection (existing) ─────────────────────────────────

    private void checkOffline(SensorNode node, LocalDateTime cutoff, LocalDateTime now) {
        boolean silent = node.getLastSeen() == null || node.getLastSeen().isBefore(cutoff);

        if (!silent || !"ONLINE".equals(node.getStatus())) {
            return;
        }

        node.setStatus("OFFLINE");
        nodeRepository.save(node);

        if (!alertAlreadyOpen(node, "DEVICE")) {
            raise(node, now,
                    node.getNodeCode() + " is offline",
                    "No data received from " + node.getNodeCode()
                            + " for more than " + offlineAfterSeconds + " seconds",
                    "DEVICE", "HIGH");
        }
    }

    // ── Battery monitoring ───────────────────────────────────────────

    void checkBattery(SensorNode node, LocalDateTime now) {
        Integer battery = node.getBatteryLevel();
        if (battery == null) {
            return;
        }

        if (battery > lowBatteryPercent) {
            return;
        }

        if (alertAlreadyOpen(node, "DEVICE")) {
            return;
        }

        boolean critical = battery <= criticalBatteryPercent;
        String severity = critical ? "HIGH" : "MEDIUM";

        raise(node, now,
                "Low battery on " + node.getNodeCode(),
                "Battery level is " + battery + "% on " + node.getNodeCode()
                        + ". " + (critical ? "Replace immediately." : "Schedule replacement."),
                "DEVICE", severity);
    }

    // ── Valve-stuck detection ────────────────────────────────────────

    void checkValveStuck(SensorNode node, LocalDateTime now) {
        String state = node.getValveState();
        if (state == null || !VALVE_MOVING.contains(state)) {
            return;
        }

        LocalDateTime lastSeen = node.getLastSeen();
        if (lastSeen == null) {
            return;
        }

        boolean stuck = lastSeen.plusSeconds(valveStuckSeconds).isBefore(now);
        if (!stuck) {
            return;
        }

        if (alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), "VALVE", "DEVICE", "RESOLVED")) {
            return;
        }

        raise(node, now,
                "Valve stuck " + state.toLowerCase() + " on " + node.getNodeCode(),
                "The valve on " + node.getNodeCode() + " has been in " + state
                        + " state for over " + valveStuckSeconds
                        + " seconds. Manual inspection required.",
                "DEVICE", "CRITICAL");
    }

    // ── helpers ──────────────────────────────────────────────────────

    private boolean alertAlreadyOpen(SensorNode node, String level) {
        return alertRepository.existsByNodeIdAndLevelAndStatusNot(
                node.getId(), level, "RESOLVED");
    }

    private void raise(SensorNode node, LocalDateTime now,
                       String title, String message, String level, String severity) {
        Zone zone = node.getZone();
        Alert alert = new Alert(null, title, message, level, severity, "OPEN",
                null, now, null, zone, node);

        Alert saved = alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", saved);
        notificationService.sendAlertEmail(saved);
    }
}

