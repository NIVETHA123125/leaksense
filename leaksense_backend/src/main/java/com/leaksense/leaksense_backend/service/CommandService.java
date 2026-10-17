package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.DeviceCommand;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.DeviceCommandRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandService {

    static final List<String> OPEN_STATUSES = List.of("PENDING", "SENT");
    static final List<String> VALVE_TYPES = List.of("CLOSE_VALVE", "OPEN_VALVE");

    private final DeviceCommandRepository commandRepository;
    private final SensorNodeRepository nodeRepository;
    private final ValveActionRepository valveActionRepository;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${leaksense.autoclose.enabled:true}")
    private boolean autoCloseEnabled;

    @Value("${leaksense.command.timeout-seconds:120}")
    private long timeoutSeconds;

    private boolean isValveCommand(String type) {
        return VALVE_TYPES.contains(type);
    }

    public ValveAction issueZoneCommand(Zone zone, String type, String source,
                                        String requestedBy, String reason) {
        boolean closing = "CLOSE_VALVE".equals(type);
        String settled = closing ? "CLOSED" : "OPEN";
        String moving = closing ? "CLOSING" : "OPENING";
        LocalDateTime now = LocalDateTime.now();
        int created = 0;

        List<SensorNode> zoneNodes = nodeRepository.findByZoneId(zone.getId());
        List<SensorNode> onlineNodes = zoneNodes.stream()
                .filter(n -> "ONLINE".equalsIgnoreCase(n.getStatus()))
                .collect(Collectors.toList());
        List<SensorNode> targetNodes = onlineNodes.isEmpty() ? zoneNodes : onlineNodes;

        for (SensorNode node : targetNodes) {
            String current = node.getValveState() == null ? "OPEN" : node.getValveState();
            if (current.equals(settled) || current.equals(moving)) {
                continue;
            }

            for (DeviceCommand old : commandRepository.findByNodeIdAndStatusIn(
                    node.getId(), OPEN_STATUSES)) {
                if (isValveCommand(old.getType())) {
                    old.setStatus("FAILED");
                    old.setResultNote("Superseded by a newer command");
                    commandRepository.save(old);
                }
            }

            commandRepository.save(new DeviceCommand(null, node, type, "PENDING", source,
                    requestedBy, reason, now, null, null, null));
            node.setValveState(moving);
            nodeRepository.save(node);
            created++;
        }

        // Keep offline nodes aligned with settled state to avoid lingering discrepancy
        for (SensorNode node : zoneNodes) {
            if (!targetNodes.contains(node)) {
                node.setValveState(settled);
                nodeRepository.save(node);
            }
        }

        if (created == 0) {
            return null;
        }

        ValveAction action = valveActionRepository.save(new ValveAction(
                null, closing ? "CLOSE" : "OPEN", requestedBy, reason,
                "AUTO".equals(source) ? "SYSTEM" : "dashboard", now, zone));
        publishValveChange(zone);
        notificationService.sendValveAction(action);
        return action;
    }

    public DeviceCommand issueNodeCommand(SensorNode node, String type, String source,
                                          String requestedBy, String reason) {
        for (DeviceCommand old : commandRepository.findByNodeIdAndStatusIn(
                node.getId(), OPEN_STATUSES)) {
            if (!isValveCommand(old.getType())) {
                old.setStatus("FAILED");
                old.setResultNote("Superseded by a newer command");
                commandRepository.save(old);
            }
        }

        return commandRepository.save(new DeviceCommand(null, node, type, "PENDING", source,
                requestedBy, reason, LocalDateTime.now(), null, null, null));
    }

    public void autoCloseZone(Zone zone, Alert alert) {
        if (!autoCloseEnabled || zone == null) {
            return;
        }
        issueZoneCommand(zone, "CLOSE_VALVE", "AUTO", "SYSTEM",
                "Auto-close: " + alert.getTitle());
    }

    public List<DeviceCommand> deliverPending(SensorNode node) {
        List<DeviceCommand> due = commandRepository.findByNodeIdAndStatusIn(
                node.getId(), OPEN_STATUSES);
        LocalDateTime now = LocalDateTime.now();

        for (DeviceCommand command : due) {
            command.setStatus("SENT");
            if (command.getSentAt() == null) {
                command.setSentAt(now);
            }
            commandRepository.save(command);
        }
        return due;
    }

    public DeviceCommand acknowledge(Long id, String nodeCode, boolean success, String note) {
        DeviceCommand command = commandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Command not found"));

        SensorNode node = command.getNode();
        if (node == null || !node.getNodeCode().equals(nodeCode)) {
            throw new IllegalArgumentException("This command belongs to a different node");
        }
        if (!OPEN_STATUSES.contains(command.getStatus())) {
            return command;
        }

        boolean valve = isValveCommand(command.getType());
        command.setAckedAt(LocalDateTime.now());
        command.setResultNote(note);

        if (success) {
            command.setStatus("ACKED");
            if (valve) {
                node.setValveState("CLOSE_VALVE".equals(command.getType()) ? "CLOSED" : "OPEN");
                resolveValveAlerts(node);
            }
        } else {
            command.setStatus("FAILED");
            if (valve) {
                node.setValveState("FAULT");
                raiseValveAlert(node, command, "reported that it could not finish the command");
            }
        }

        commandRepository.save(command);

        if (valve) {
            nodeRepository.save(node);
            if (node.getZone() != null) {
                publishValveChange(node.getZone());
            }
        }
        return command;
    }

    public void simulateDeviceReplies(SensorNode node) {
        for (DeviceCommand command : commandRepository.findByNodeIdAndStatusIn(
                node.getId(), OPEN_STATUSES)) {
            acknowledge(command.getId(), node.getNodeCode(), true, "Simulated device");
        }
    }

    @Scheduled(fixedRate = 30000)
    public void expireStale() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);

        for (DeviceCommand command : commandRepository.findByStatusIn(OPEN_STATUSES)) {
            if (command.getCreatedAt() == null || !command.getCreatedAt().isBefore(cutoff)) {
                continue;
            }

            command.setStatus("FAILED");
            command.setResultNote("No confirmation from the device within "
                    + timeoutSeconds + " seconds");
            commandRepository.save(command);

            SensorNode node = command.getNode();
            if (node != null && isValveCommand(command.getType())) {
                node.setValveState("FAULT");
                nodeRepository.save(node);
                raiseValveAlert(node, command,
                        "was not confirmed within " + timeoutSeconds + " seconds");
                if (node.getZone() != null) {
                    publishValveChange(node.getZone());
                }
            }
        }
    }

    private void resolveValveAlerts(SensorNode node) {
        for (Alert alert : alertRepository.findByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), "VALVE", "DEVICE", "RESOLVED")) {
            alert.setStatus("RESOLVED");
            alert.setResolvedAt(LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/alerts", alertRepository.save(alert));
        }
    }

    private void raiseValveAlert(SensorNode node, DeviceCommand command, String problem) {
        if (alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                node.getId(), "VALVE", "DEVICE", "RESOLVED")) {
            return;
        }

        boolean closing = "CLOSE_VALVE".equals(command.getType());
        Alert alert = new Alert(null,
                "Valve " + (closing ? "close" : "open") + " failed on " + node.getNodeCode(),
                "The valve command on " + node.getNodeCode() + " " + problem
                        + ". Check the valve manually.",
                "DEVICE", closing ? "CRITICAL" : "HIGH", "OPEN", null,
                LocalDateTime.now(), null, node.getZone(), node, "VALVE");

        Alert saved = alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", saved);
        notificationService.sendAlertEmail(saved);
    }

    private void publishValveChange(Zone zone) {
        messagingTemplate.convertAndSend("/topic/valves",
                Optional.of(Map.of("zoneId", zone.getId(), "zone", zone.getName())));
    }
}