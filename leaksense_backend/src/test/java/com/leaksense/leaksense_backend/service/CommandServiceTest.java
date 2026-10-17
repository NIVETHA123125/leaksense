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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private DeviceCommandRepository commandRepository;

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CommandService service;

    private Zone zone;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "autoCloseEnabled", true);
        ReflectionTestUtils.setField(service, "timeoutSeconds", 120L);
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
    }

    private SensorNode node(Long id, String code, String valve) {
        return new SensorNode(id, code, "ONLINE", 80, LocalDateTime.now(), zone, valve);
    }

    private DeviceCommand command(Long id, SensorNode node, String type, String status,
                                  LocalDateTime createdAt) {
        return new DeviceCommand(id, node, type, status, "AUTO", "SYSTEM", "reason",
                createdAt, null, null, null);
    }

    private Alert leakAlert() {
        return new Alert(1L, "High methane in Zone A", "msg", "SENSOR", "CRITICAL", "OPEN",
                null, LocalDateTime.now(), null, zone, null);
    }

    @Test
    @DisplayName("Closing a zone creates one pending command per open valve")
    void closeCreatesCommandPerOpenValve() {
        SensorNode a1 = node(1L, "NODE-A1", "OPEN");
        SensorNode a2 = node(2L, "NODE-A2", null);
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of(a1, a2));
        when(valveActionRepository.save(any(ValveAction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ValveAction result = service.issueZoneCommand(
                zone, "CLOSE_VALVE", "MANUAL", "admin", "Manual close from dashboard");

        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commandRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(c -> {
            assertThat(c.getType()).isEqualTo("CLOSE_VALVE");
            assertThat(c.getStatus()).isEqualTo("PENDING");
            assertThat(c.getSource()).isEqualTo("MANUAL");
            assertThat(c.getRequestedBy()).isEqualTo("admin");
        });
        assertThat(a1.getValveState()).isEqualTo("CLOSING");
        assertThat(a2.getValveState()).isEqualTo("CLOSING");
        assertThat(result.getAction()).isEqualTo("CLOSE");
        assertThat(result.getTriggeredBy()).isEqualTo("admin");
        assertThat(result.getZone()).isSameAs(zone);
    }

    @Test
    @DisplayName("Valves that are already closed or closing are skipped")
    void alreadyClosedValvesAreSkipped() {
        SensorNode closed = node(1L, "NODE-A1", "CLOSED");
        SensorNode closing = node(2L, "NODE-A2", "CLOSING");
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of(closed, closing));

        ValveAction result = service.issueZoneCommand(
                zone, "CLOSE_VALVE", "MANUAL", "admin", "Manual close from dashboard");

        assertThat(result).isNull();
        verify(commandRepository, never()).save(any());
        verify(valveActionRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("A newer command cancels an unfinished older one")
    void newerCommandSupersedesOlder() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand oldClose = command(10L, n, "CLOSE_VALVE", "PENDING", LocalDateTime.now());
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of(n));
        when(commandRepository.findByNodeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(oldClose));
        when(valveActionRepository.save(any(ValveAction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.issueZoneCommand(zone, "OPEN_VALVE", "MANUAL", "admin", "Manual open from dashboard");

        assertThat(oldClose.getStatus()).isEqualTo("FAILED");
        assertThat(oldClose.getResultNote()).contains("Superseded");
        assertThat(n.getValveState()).isEqualTo("OPENING");
    }

    @Test
    @DisplayName("Auto-close does nothing when it is switched off")
    void autoCloseDisabled() {
        ReflectionTestUtils.setField(service, "autoCloseEnabled", false);

        service.autoCloseZone(zone, leakAlert());

        verifyNoInteractions(nodeRepository, commandRepository, valveActionRepository);
    }

    @Test
    @DisplayName("Auto-close does nothing for an alert without a zone")
    void autoCloseWithoutZone() {
        service.autoCloseZone(null, leakAlert());

        verifyNoInteractions(nodeRepository, commandRepository, valveActionRepository);
    }

    @Test
    @DisplayName("Auto-close creates SYSTEM commands with the alert title as the reason")
    void autoCloseCreatesSystemCommand() {
        SensorNode n = node(1L, "NODE-A1", "OPEN");
        when(nodeRepository.findByZoneId(1L)).thenReturn(List.of(n));

        service.autoCloseZone(zone, leakAlert());

        ArgumentCaptor<DeviceCommand> commandCaptor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commandRepository).save(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getSource()).isEqualTo("AUTO");
        assertThat(commandCaptor.getValue().getRequestedBy()).isEqualTo("SYSTEM");
        assertThat(commandCaptor.getValue().getReason())
                .isEqualTo("Auto-close: High methane in Zone A");

        ArgumentCaptor<ValveAction> actionCaptor = ArgumentCaptor.forClass(ValveAction.class);
        verify(valveActionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getTriggeredBy()).isEqualTo("SYSTEM");
        assertThat(actionCaptor.getValue().getAction()).isEqualTo("CLOSE");
    }

    @Test
    @DisplayName("Delivering commands marks them SENT")
    void deliverPendingMarksSent() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "PENDING", LocalDateTime.now());
        when(commandRepository.findByNodeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(c));

        List<DeviceCommand> result = service.deliverPending(n);

        assertThat(result).containsExactly(c);
        assertThat(c.getStatus()).isEqualTo("SENT");
        assertThat(c.getSentAt()).isNotNull();
        verify(commandRepository).save(c);
    }

    @Test
    @DisplayName("A successful confirmation closes the valve")
    void ackSuccessClosesValve() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));

        DeviceCommand result = service.acknowledge(5L, "NODE-A1", true, "closed");

        assertThat(result.getStatus()).isEqualTo("ACKED");
        assertThat(result.getAckedAt()).isNotNull();
        assertThat(n.getValveState()).isEqualTo("CLOSED");
        verify(nodeRepository).save(n);
    }

    @Test
    @DisplayName("A successful open confirmation opens the valve")
    void ackSuccessOpensValve() {
        SensorNode n = node(1L, "NODE-A1", "OPENING");
        DeviceCommand c = command(6L, n, "OPEN_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findById(6L)).thenReturn(Optional.of(c));

        service.acknowledge(6L, "NODE-A1", true, null);

        assertThat(n.getValveState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("A successful confirmation closes an open valve-failed alert")
    void ackSuccessResolvesValveAlert() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        Alert valveAlert = new Alert(9L, "Valve close failed on NODE-A1", "msg", "DEVICE",
                "CRITICAL", "OPEN", null, LocalDateTime.now(), null, zone, n, "VALVE");
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));
        when(alertRepository.findByNodeIdAndSensorAndLevelAndStatusNot(
                1L, "VALVE", "DEVICE", "RESOLVED")).thenReturn(List.of(valveAlert));

        service.acknowledge(5L, "NODE-A1", true, null);

        assertThat(valveAlert.getStatus()).isEqualTo("RESOLVED");
        assertThat(valveAlert.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("A failed confirmation sets FAULT and raises a CRITICAL alert")
    void ackFailureRaisesAlert() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));

        service.acknowledge(5L, "NODE-A1", false, "valve stuck");

        assertThat(c.getStatus()).isEqualTo("FAILED");
        assertThat(n.getValveState()).isEqualTo("FAULT");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert alert = captor.getValue();
        assertThat(alert.getTitle()).isEqualTo("Valve close failed on NODE-A1");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getLevel()).isEqualTo("DEVICE");
        assertThat(alert.getSensor()).isEqualTo("VALVE");
    }

    @Test
    @DisplayName("A confirmation from the wrong node is rejected")
    void ackFromWrongNode() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.acknowledge(5L, "NODE-B1", true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This command belongs to a different node");
    }

    @Test
    @DisplayName("A confirmation for an unknown command is rejected")
    void ackUnknownCommand() {
        when(commandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(99L, "NODE-A1", true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Command not found");
    }

    @Test
    @DisplayName("A finished command cannot be changed by a late confirmation")
    void lateAckIsIgnored() {
        SensorNode n = node(1L, "NODE-A1", "FAULT");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "FAILED", LocalDateTime.now());
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));

        DeviceCommand result = service.acknowledge(5L, "NODE-A1", true, null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(n.getValveState()).isEqualTo("FAULT");
        verify(nodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Only commands older than the timeout fail")
    void expireStaleFailsOnlyOldCommands() {
        SensorNode slow = node(1L, "NODE-A1", "CLOSING");
        SensorNode fresh = node(2L, "NODE-A2", "CLOSING");
        DeviceCommand old = command(1L, slow, "CLOSE_VALVE", "SENT",
                LocalDateTime.now().minusSeconds(300));
        DeviceCommand recent = command(2L, fresh, "CLOSE_VALVE", "PENDING",
                LocalDateTime.now().minusSeconds(10));
        when(commandRepository.findByStatusIn(anyCollection())).thenReturn(List.of(old, recent));

        service.expireStale();

        assertThat(old.getStatus()).isEqualTo("FAILED");
        assertThat(old.getResultNote()).contains("120 seconds");
        assertThat(slow.getValveState()).isEqualTo("FAULT");
        assertThat(recent.getStatus()).isEqualTo("PENDING");
        assertThat(fresh.getValveState()).isEqualTo("CLOSING");
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    @DisplayName("The simulator confirms every unfinished command of its node")
    void simulatedRepliesConfirmCommands() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand c = command(5L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findByNodeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(c));
        when(commandRepository.findById(5L)).thenReturn(Optional.of(c));

        service.simulateDeviceReplies(n);

        assertThat(c.getStatus()).isEqualTo("ACKED");
        assertThat(n.getValveState()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("A node command is queued without touching the valve state")
    void nodeCommandIsQueued() {
        SensorNode n = node(1L, "NODE-A1", "OPEN");
        when(commandRepository.save(any(DeviceCommand.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeviceCommand result = service.issueNodeCommand(
                n, "ALARM_ON", "AUTO", "SYSTEM", "Critical methane");

        assertThat(result.getType()).isEqualTo("ALARM_ON");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getNode()).isSameAs(n);
        assertThat(n.getValveState()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("A new alarm command cancels older alarm commands but never valve commands")
    void alarmCommandSupersedesOnlyAlarmCommands() {
        SensorNode n = node(1L, "NODE-A1", "CLOSING");
        DeviceCommand oldAlarm = command(10L, n, "ALARM_ON", "PENDING", LocalDateTime.now());
        DeviceCommand valveCommand = command(11L, n, "CLOSE_VALVE", "SENT", LocalDateTime.now());
        when(commandRepository.findByNodeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(List.of(oldAlarm, valveCommand));
        when(commandRepository.save(any(DeviceCommand.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.issueNodeCommand(n, "SILENCE_ALARM", "MANUAL", "operator", "Silenced");

        assertThat(oldAlarm.getStatus()).isEqualTo("FAILED");
        assertThat(valveCommand.getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("Confirming an alarm command leaves the valve state alone")
    void alarmAckDoesNotTouchValve() {
        SensorNode n = node(1L, "NODE-A1", "CLOSED");
        DeviceCommand c = command(7L, n, "ALARM_ON", "SENT", LocalDateTime.now());
        when(commandRepository.findById(7L)).thenReturn(Optional.of(c));

        service.acknowledge(7L, "NODE-A1", true, null);

        assertThat(c.getStatus()).isEqualTo("ACKED");
        assertThat(n.getValveState()).isEqualTo("CLOSED");
        verify(nodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("A failed alarm command raises no valve alert")
    void alarmFailureRaisesNoValveAlert() {
        SensorNode n = node(1L, "NODE-A1", "OPEN");
        DeviceCommand c = command(7L, n, "SILENCE_ALARM", "SENT", LocalDateTime.now());
        when(commandRepository.findById(7L)).thenReturn(Optional.of(c));

        service.acknowledge(7L, "NODE-A1", false, "buzzer stuck");

        assertThat(c.getStatus()).isEqualTo("FAILED");
        assertThat(n.getValveState()).isEqualTo("OPEN");
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unconfirmed alarm command expires without a valve fault")
    void expiredAlarmCommandHasNoValveFault() {
        SensorNode n = node(1L, "NODE-A1", "OPEN");
        DeviceCommand old = command(1L, n, "ALARM_ON", "SENT",
                LocalDateTime.now().minusSeconds(300));
        when(commandRepository.findByStatusIn(anyCollection())).thenReturn(List.of(old));

        service.expireStale();

        assertThat(old.getStatus()).isEqualTo("FAILED");
        assertThat(n.getValveState()).isEqualTo("OPEN");
        verify(alertRepository, never()).save(any());
    }
}