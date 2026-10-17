package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.AlarmEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    static final List<String> OPEN_STATUSES = List.of("ACTIVE", "SILENCED");

    private final AlarmEventRepository alarmRepository;
    private final CommandService commandService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Value("${leaksense.alarm.resound-minutes:5}")
    private long reSoundMinutes;

    public void evaluate(SensorNode node, ThresholdEvaluator.Evaluation evaluation,
                         LocalDateTime now) {
        Optional<ThresholdEvaluator.SensorCheck> critical = evaluation.all().stream()
                .filter(check -> check.status() == ThresholdEvaluator.Status.CRITICAL)
                .findFirst();
        Optional<AlarmEvent> open = alarmRepository.findFirstByNodeIdAndStatusIn(
                node.getId(), OPEN_STATUSES);

        if (critical.isEmpty()) {
            open.ifPresent(alarm -> clear(alarm, now));
            return;
        }

        if (open.isEmpty()) {
            start(node, critical.get(), now);
            return;
        }

        AlarmEvent alarm = open.get();
        boolean mutedTooLong = "SILENCED".equals(alarm.getStatus())
                && alarm.getSilencedAt() != null
                && alarm.getSilencedAt().plusMinutes(reSoundMinutes).isBefore(now);

        if (mutedTooLong) {
            finish(alarm, now);
            start(node, critical.get(), now);
        }
    }

    public AlarmEvent silence(Long id, String username) {
        AlarmEvent alarm = alarmRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alarm not found"));

        if ("CLEARED".equals(alarm.getStatus())) {
            throw new IllegalArgumentException("This alarm has already cleared");
        }

        silenceOne(alarm, username);
        return alarm;
    }

    public int silenceAll(String username) {
        List<AlarmEvent> active = alarmRepository.findByStatusInOrderByStartedAtDesc(
                List.of("ACTIVE"));

        for (AlarmEvent alarm : active) {
            silenceOne(alarm, username);
        }
        return active.size();
    }

    public List<AlarmEvent> active() {
        return alarmRepository.findByStatusInOrderByStartedAtDesc(OPEN_STATUSES);
    }

    public List<AlarmEvent> recent() {
        return alarmRepository.findTop20ByOrderByStartedAtDesc();
    }

    private void silenceOne(AlarmEvent alarm, String username) {
        if ("SILENCED".equals(alarm.getStatus())) {
            return;
        }

        alarm.setStatus("SILENCED");
        alarm.setSilencedAt(LocalDateTime.now());
        alarm.setSilencedBy(username);
        AlarmEvent saved = alarmRepository.save(alarm);

        commandService.issueNodeCommand(alarm.getNode(), "SILENCE_ALARM", "MANUAL",
                username, "Silenced from dashboard");
        notificationService.sendAlarmSilenced(saved, username);
        publish();
    }

    private void start(SensorNode node, ThresholdEvaluator.SensorCheck check, LocalDateTime now) {
        String message = describe(check);

        AlarmEvent saved = alarmRepository.save(new AlarmEvent(null, node, node.getZone(), check.sensor(),
                message, "ACTIVE", now, null, null, null));
        commandService.issueNodeCommand(node, "ALARM_ON", "AUTO", "SYSTEM", message);
        notificationService.sendAlarmTriggered(saved);
        publish();
    }

    private void clear(AlarmEvent alarm, LocalDateTime now) {
        finish(alarm, now);
        commandService.issueNodeCommand(alarm.getNode(), "ALARM_OFF", "AUTO", "SYSTEM",
                "Danger cleared");
        publish();
    }

    private void finish(AlarmEvent alarm, LocalDateTime now) {
        alarm.setStatus("CLEARED");
        alarm.setClearedAt(now);
        AlarmEvent saved = alarmRepository.save(alarm);
        notificationService.sendAlarmCleared(saved);
    }

    private String describe(ThresholdEvaluator.SensorCheck check) {
        return String.format(Locale.ROOT, "%s %s at %.1f %s (critical limit %.1f %s)",
                check.label(),
                "HIGH".equals(check.direction()) ? "high" : "low",
                check.value(), check.unit(), check.limit(), check.unit());
    }

    private void publish() {
        messagingTemplate.convertAndSend("/topic/alarms", Optional.of(Map.of("type", "alarms")));
    }
}
