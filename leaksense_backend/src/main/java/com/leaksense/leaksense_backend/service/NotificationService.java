package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.AppNotification;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final JavaMailSender mailSender;
    private final AppNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SmsService smsService;

    @Value("${leaksense.notify.enabled:false}")
    private boolean enabled;

    @Value("${leaksense.notify.to:}")
    private String recipients;

    @Value("${spring.mail.username:}")
    private String from;

    public AppNotification createInAppNotification(String title, String message, String type,
                                                   String severity, Long zoneId, Long nodeId) {
        try {
            AppNotification notification = AppNotification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .severity(severity)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .zoneId(zoneId)
                    .nodeId(nodeId)
                    .build();
            AppNotification saved = notificationRepository.save(notification);
            messagingTemplate.convertAndSend("/topic/notifications", saved);
            return saved;
        } catch (Exception e) {
            log.warn("Could not save or broadcast in-app notification: {}", e.getMessage());
            return null;
        }
    }

    @Async
    public void sendAlertEmail(Alert alert) {
        if (alert == null) return;

        Long zoneId = alert.getZone() != null ? alert.getZone().getId() : null;
        Long nodeId = alert.getNode() != null ? alert.getNode().getId() : null;
        createInAppNotification(alert.getTitle(), alert.getMessage(), "ALERT", alert.getSeverity(), zoneId, nodeId);

        if ("CRITICAL".equalsIgnoreCase(alert.getSeverity())) {
            smsService.sendAlertSms(alert);
        }

        if (!canSendEmail()) return;

        try {
            String zone = alert.getZone() == null ? "-" : alert.getZone().getName();
            String node = alert.getNode() == null ? "-" : alert.getNode().getNodeCode();

            String time = alert.getCreatedAt() != null ? alert.getCreatedAt().format(FORMAT) : LocalDateTime.now().format(FORMAT);
            String body = "LeakSense alert\n\n"
                    + "Title:    " + alert.getTitle() + "\n"
                    + "Details:  " + alert.getMessage() + "\n"
                    + "Severity: " + alert.getSeverity() + "\n"
                    + "Type:     " + alert.getLevel() + "\n"
                    + "Zone:     " + zone + "\n"
                    + "Node:     " + node + "\n"
                    + "Time:     " + time + "\n\n"
                    + "Open the LeakSense dashboard to acknowledge or resolve this alert.";

            sendMail("[LeakSense] " + alert.getSeverity() + ": " + alert.getTitle(), body);
            log.info("Alert email sent for: {}", alert.getTitle());
        } catch (Exception e) {
            log.warn("Could not send alert email: {}", e.getMessage());
        }
    }

    @Async
    public void sendAlarmTriggered(AlarmEvent alarm) {
        if (alarm == null) return;

        Long zoneId = alarm.getZone() != null ? alarm.getZone().getId() : null;
        Long nodeId = alarm.getNode() != null ? alarm.getNode().getId() : null;
        String zone = alarm.getZone() != null ? alarm.getZone().getName() : "-";
        String node = alarm.getNode() != null ? alarm.getNode().getNodeCode() : "-";

        createInAppNotification("CRITICAL ALARM: " + alarm.getSensor(), alarm.getMessage(), "ALARM", "CRITICAL", zoneId, nodeId);
        smsService.sendAlarmSms(alarm);

        if (!canSendEmail()) return;

        String body = "CRITICAL ALARM TRIGGERED\n\n"
                + "Sensor:   " + alarm.getSensor() + "\n"
                + "Details:  " + alarm.getMessage() + "\n"
                + "Zone:     " + zone + "\n"
                + "Node:     " + node + "\n"
                + "Status:   " + alarm.getStatus() + "\n"
                + "Started:  " + (alarm.getStartedAt() != null ? alarm.getStartedAt().format(FORMAT) : "-") + "\n\n"
                + "IMMEDIATE ATTENTION REQUIRED. Open the dashboard to inspect or silence.";

        sendMail("[LeakSense] CRITICAL ALARM: " + alarm.getSensor() + " in " + zone, body);
    }

    @Async
    public void sendAlarmSilenced(AlarmEvent alarm, String silencedBy) {
        if (alarm == null) return;

        Long zoneId = alarm.getZone() != null ? alarm.getZone().getId() : null;
        Long nodeId = alarm.getNode() != null ? alarm.getNode().getId() : null;
        String zone = alarm.getZone() != null ? alarm.getZone().getName() : "-";
        String node = alarm.getNode() != null ? alarm.getNode().getNodeCode() : "-";

        createInAppNotification("Alarm Silenced", "Alarm on " + node + " was silenced by " + silencedBy, "ALARM", "WARNING", zoneId, nodeId);

        if (!canSendEmail()) return;

        String body = "ALARM SILENCED\n\n"
                + "Sensor:       " + alarm.getSensor() + "\n"
                + "Silenced By:  " + silencedBy + "\n"
                + "Silenced At:  " + (alarm.getSilencedAt() != null ? alarm.getSilencedAt().format(FORMAT) : "-") + "\n"
                + "Zone:         " + zone + "\n"
                + "Node:         " + node + "\n\n"
                + "Note: If hazard persists, the alarm will re-sound after the silence timeout.";

        sendMail("[LeakSense] Alarm Silenced by " + silencedBy + " (" + node + ")", body);
    }

    @Async
    public void sendAlarmCleared(AlarmEvent alarm) {
        if (alarm == null) return;

        Long zoneId = alarm.getZone() != null ? alarm.getZone().getId() : null;
        Long nodeId = alarm.getNode() != null ? alarm.getNode().getId() : null;
        String zone = alarm.getZone() != null ? alarm.getZone().getName() : "-";
        String node = alarm.getNode() != null ? alarm.getNode().getNodeCode() : "-";

        createInAppNotification("Alarm Cleared", "Hazard cleared on " + node + " (" + alarm.getSensor() + ")", "ALARM", "INFO", zoneId, nodeId);

        if (!canSendEmail()) return;

        String body = "ALARM CLEARED\n\n"
                + "Sensor:      " + alarm.getSensor() + "\n"
                + "Zone:        " + zone + "\n"
                + "Node:        " + node + "\n"
                + "Cleared At:  " + (alarm.getClearedAt() != null ? alarm.getClearedAt().format(FORMAT) : "-") + "\n\n"
                + "Readings have returned to safe thresholds.";

        sendMail("[LeakSense] Alarm Cleared: " + alarm.getSensor() + " in " + zone, body);
    }

    @Async
    public void sendNodeRestored(SensorNode node) {
        if (node == null) return;

        Long zoneId = node.getZone() != null ? node.getZone().getId() : null;
        String zone = node.getZone() != null ? node.getZone().getName() : "-";

        createInAppNotification("Node Online", "Sensor node " + node.getNodeCode() + " is back online and communicating.", "NODE", "INFO", zoneId, node.getId());

        if (!canSendEmail()) return;

        String body = "SENSOR NODE RESTORED\n\n"
                + "Node Code:   " + node.getNodeCode() + "\n"
                + "Zone:        " + zone + "\n"
                + "Battery:     " + (node.getBatteryLevel() != null ? node.getBatteryLevel() + "%" : "-") + "\n"
                + "Last Seen:   " + (node.getLastSeen() != null ? node.getLastSeen().format(FORMAT) : "-") + "\n\n"
                + "Node is now actively transmitting sensor readings.";

        sendMail("[LeakSense] Node Restored: " + node.getNodeCode() + " (" + zone + ")", body);
    }

    @Async
    public void sendValveAction(ValveAction action) {
        if (action == null) return;

        Long zoneId = action.getZone() != null ? action.getZone().getId() : null;
        String zone = action.getZone() != null ? action.getZone().getName() : "-";
        String severity = "CLOSE".equalsIgnoreCase(action.getAction()) ? "WARNING" : "INFO";

        createInAppNotification("Valve " + action.getAction(),
                "Valve in " + zone + " " + action.getAction() + "ED (" + action.getReason() + ")",
                "VALVE", severity, zoneId, null);

        if (!canSendEmail()) return;

        String body = "VALVE ACTION EXECUTED\n\n"
                + "Action:        " + action.getAction() + "\n"
                + "Zone:          " + zone + "\n"
                + "Triggered By:  " + action.getTriggeredBy() + "\n"
                + "Reason:        " + action.getReason() + "\n"
                + "Time:          " + (action.getPerformedAt() != null ? action.getPerformedAt().format(FORMAT) : "-") + "\n";

        sendMail("[LeakSense] Valve " + action.getAction() + ": " + zone, body);
    }

    @Async
    public void sendThresholdChanged(String username, String details) {
        createInAppNotification("Thresholds Updated",
                "Safety thresholds modified by " + (username != null ? username : "Administrator")
                        + (details != null && !details.isBlank() ? " (" + details + ")" : "."),
                "CONFIG", "INFO", null, null);

        if (!canSendEmail()) return;

        String body = "SAFETY THRESHOLDS MODIFIED\n\n"
                + "Updated By:  " + (username != null ? username : "Administrator") + "\n"
                + "Details:     " + (details != null ? details : "System threshold settings reconfigured") + "\n"
                + "Timestamp:   " + LocalDateTime.now().format(FORMAT) + "\n\n"
                + "Please review the updated safety parameters in the settings panel.";

        sendMail("[LeakSense] Safety Thresholds Updated", body);
    }

    private boolean canSendEmail() {
        return enabled && recipients != null && !recipients.isBlank() && from != null && !from.isBlank();
    }

    private void sendMail(String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients.trim().split("\\s*,\\s*"));
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Could not dispatch email [{}]: {}", subject, e.getMessage());
        }
    }
}
