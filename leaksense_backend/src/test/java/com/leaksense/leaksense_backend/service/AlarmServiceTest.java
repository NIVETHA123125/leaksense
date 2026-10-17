package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlarmEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmEventRepository alarmRepository;

    @Mock
    private CommandService commandService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AlarmService service;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "reSoundMinutes", 5L);
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 80, LocalDateTime.now(), zone, "OPEN");
    }

    private ThresholdEvaluator.SensorCheck check(String sensor, ThresholdEvaluator.Status status) {
        String direction = status == ThresholdEvaluator.Status.NORMAL ? null : "HIGH";
        return new ThresholdEvaluator.SensorCheck(sensor, "Methane", "ppm", status,
                direction, 400.0, 350.0);
    }

    private ThresholdEvaluator.Evaluation evaluation(ThresholdEvaluator.Status methane,
                                                     ThresholdEvaluator.Status pressure) {
        return new ThresholdEvaluator.Evaluation(
                check("METHANE", methane),
                check("PRESSURE", pressure),
                check("OXYGEN", ThresholdEvaluator.Status.NORMAL));
    }

    private AlarmEvent alarm(Long id, String status, LocalDateTime silencedAt) {
        return new AlarmEvent(id, node, zone, "METHANE", "Methane high", status,
                LocalDateTime.now().minusMinutes(10), silencedAt, null, null);
    }

    private void givenOpenAlarm(AlarmEvent alarm) {
        when(alarmRepository.findFirstByNodeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(alarm));
    }

    @Test
    @DisplayName("A critical sensor starts an alarm and queues ALARM_ON")
    void criticalStartsAlarm() {
        LocalDateTime now = LocalDateTime.now();

        service.evaluate(node, evaluation(ThresholdEvaluator.Status.CRITICAL,
                ThresholdEvaluator.Status.NORMAL), now);

        ArgumentCaptor<AlarmEvent> captor = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarmRepository).save(captor.capture());
        AlarmEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getSensor()).isEqualTo("METHANE");
        assertThat(saved.getZone()).isSameAs(zone);
        assertThat(saved.getStartedAt()).isEqualTo(now);
        assertThat(saved.getMessage()).contains("Methane high at 400.0 ppm")
                .contains("critical limit 350.0 ppm");

        verify(commandService).issueNodeCommand(eq(node), eq("ALARM_ON"), eq("AUTO"),
                eq("SYSTEM"), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/alarms"), any(Optional.class));
    }

    @Test
    @DisplayName("Warnings alone never start an alarm")
    void warningsDoNotStartAlarm() {
        service.evaluate(node, evaluation(ThresholdEvaluator.Status.WARNING,
                ThresholdEvaluator.Status.WARNING), LocalDateTime.now());

        verify(alarmRepository, never()).save(any());
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("A sensor fault never starts an alarm")
    void faultsDoNotStartAlarm() {
        service.evaluate(node, evaluation(ThresholdEvaluator.Status.FAULT,
                ThresholdEvaluator.Status.NORMAL), LocalDateTime.now());

        verify(alarmRepository, never()).save(any());
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("An alarm that is already active is not started twice")
    void noDuplicateAlarm() {
        givenOpenAlarm(alarm(5L, "ACTIVE", null));

        service.evaluate(node, evaluation(ThresholdEvaluator.Status.CRITICAL,
                ThresholdEvaluator.Status.NORMAL), LocalDateTime.now());

        verify(alarmRepository, never()).save(any());
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("Normal readings clear an open alarm and queue ALARM_OFF")
    void normalReadingsClearAlarm() {
        AlarmEvent open = alarm(5L, "ACTIVE", null);
        givenOpenAlarm(open);
        LocalDateTime now = LocalDateTime.now();

        service.evaluate(node, evaluation(ThresholdEvaluator.Status.NORMAL,
                ThresholdEvaluator.Status.NORMAL), now);

        assertThat(open.getStatus()).isEqualTo("CLEARED");
        assertThat(open.getClearedAt()).isEqualTo(now);
        verify(commandService).issueNodeCommand(eq(node), eq("ALARM_OFF"), eq("AUTO"),
                eq("SYSTEM"), anyString());
    }

    @Test
    @DisplayName("A silenced alarm stays silent before the re-sound time")
    void silencedAlarmStaysSilent() {
        AlarmEvent silenced = alarm(5L, "SILENCED", LocalDateTime.now().minusMinutes(2));
        givenOpenAlarm(silenced);

        service.evaluate(node, evaluation(ThresholdEvaluator.Status.CRITICAL,
                ThresholdEvaluator.Status.NORMAL), LocalDateTime.now());

        assertThat(silenced.getStatus()).isEqualTo("SILENCED");
        verify(alarmRepository, never()).save(any());
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("A silenced alarm re-sounds as a new alarm after the re-sound time")
    void silencedAlarmReSounds() {
        AlarmEvent silenced = alarm(5L, "SILENCED", LocalDateTime.now().minusMinutes(6));
        givenOpenAlarm(silenced);

        service.evaluate(node, evaluation(ThresholdEvaluator.Status.CRITICAL,
                ThresholdEvaluator.Status.NORMAL), LocalDateTime.now());

        ArgumentCaptor<AlarmEvent> captor = ArgumentCaptor.forClass(AlarmEvent.class);
        verify(alarmRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo("CLEARED");
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo("ACTIVE");
        verify(commandService).issueNodeCommand(eq(node), eq("ALARM_ON"), eq("AUTO"),
                eq("SYSTEM"), anyString());
    }

    @Test
    @DisplayName("Silencing records who did it and queues SILENCE_ALARM")
    void silenceRecordsUserAndQueuesCommand() {
        AlarmEvent active = alarm(5L, "ACTIVE", null);
        when(alarmRepository.findById(5L)).thenReturn(Optional.of(active));

        service.silence(5L, "operator");

        assertThat(active.getStatus()).isEqualTo("SILENCED");
        assertThat(active.getSilencedBy()).isEqualTo("operator");
        assertThat(active.getSilencedAt()).isNotNull();
        verify(alarmRepository).save(active);
        verify(commandService).issueNodeCommand(eq(node), eq("SILENCE_ALARM"), eq("MANUAL"),
                eq("operator"), anyString());
    }

    @Test
    @DisplayName("Silencing an already silenced alarm changes nothing")
    void silenceTwiceDoesNothing() {
        AlarmEvent silenced = alarm(5L, "SILENCED", LocalDateTime.now().minusMinutes(1));
        when(alarmRepository.findById(5L)).thenReturn(Optional.of(silenced));

        service.silence(5L, "admin");

        verify(alarmRepository, never()).save(any());
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("A cleared alarm cannot be silenced")
    void clearedAlarmCannotBeSilenced() {
        when(alarmRepository.findById(5L)).thenReturn(Optional.of(alarm(5L, "CLEARED", null)));

        assertThatThrownBy(() -> service.silence(5L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This alarm has already cleared");
    }

    @Test
    @DisplayName("An unknown alarm is rejected")
    void unknownAlarm() {
        when(alarmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.silence(99L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alarm not found");
    }

    @Test
    @DisplayName("Silence all silences every active alarm")
    void silenceAllSilencesEveryAlarm() {
        AlarmEvent first = alarm(5L, "ACTIVE", null);
        AlarmEvent second = alarm(6L, "ACTIVE", null);
        when(alarmRepository.findByStatusInOrderByStartedAtDesc(anyCollection()))
                .thenReturn(List.of(first, second));

        int count = service.silenceAll("operator");

        assertThat(count).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo("SILENCED");
        assertThat(second.getStatus()).isEqualTo("SILENCED");
        verify(commandService, times(2)).issueNodeCommand(eq(node), eq("SILENCE_ALARM"),
                eq("MANUAL"), eq("operator"), anyString());
    }
}
