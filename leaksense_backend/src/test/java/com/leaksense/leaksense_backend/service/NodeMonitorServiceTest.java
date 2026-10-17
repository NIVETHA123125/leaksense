package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeMonitorServiceTest {

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NodeMonitorService monitor;

    private Zone zone;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monitor, "offlineAfterSeconds", 60L);
        ReflectionTestUtils.setField(monitor, "lowBatteryPercent", 20);
        ReflectionTestUtils.setField(monitor, "criticalBatteryPercent", 10);
        ReflectionTestUtils.setField(monitor, "valveStuckSeconds", 120L);
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
    }

    private SensorNode node(Long id, String code, String status, LocalDateTime lastSeen) {
        return new SensorNode(id, code, status, 80, lastSeen, zone);
    }

    // ── Offline detection (existing tests) ───────────────────────────

    @Test
    @DisplayName("A silent online node goes OFFLINE and raises a DEVICE alert")
    void silentNodeGoesOfflineAndRaisesAlert() {
        SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(120));
        when(nodeRepository.findAll()).thenReturn(List.of(n));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo("OFFLINE");
        verify(nodeRepository).save(n);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert alert = captor.getValue();

        assertThat(alert.getTitle()).isEqualTo("NODE-A1 is offline");
        assertThat(alert.getLevel()).isEqualTo("DEVICE");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        assertThat(alert.getStatus()).isEqualTo("OPEN");
        assertThat(alert.getNode()).isSameAs(n);
        assertThat(alert.getZone()).isSameAs(zone);

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Alert.class));
        verify(notificationService).sendAlertEmail(alert);
    }

    @Test
    @DisplayName("A node that reported recently stays ONLINE")
    void recentNodeStaysOnline() {
        SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(10));
        when(nodeRepository.findAll()).thenReturn(List.of(n));

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo("ONLINE");
        verify(nodeRepository, never()).save(any(SensorNode.class));
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("A node that never reported goes OFFLINE")
    void neverSeenNodeGoesOffline() {
        SensorNode n = node(1L, "NODE-A1", "ONLINE", null);
        when(nodeRepository.findAll()).thenReturn(List.of(n));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo("OFFLINE");
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("A node that is already OFFLINE does not raise another alert")
    void alreadyOfflineNodeIsIgnored() {
        SensorNode n = node(1L, "NODE-B1", "OFFLINE", LocalDateTime.now().minusHours(3));
        when(nodeRepository.findAll()).thenReturn(List.of(n));

        monitor.checkNodes();

        verify(nodeRepository, never()).save(any(SensorNode.class));
        verify(alertRepository, never()).save(any(Alert.class));
        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    @DisplayName("An open DEVICE alert is not duplicated")
    void openDeviceAlertIsNotDuplicated() {
        SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(120));
        when(nodeRepository.findAll()).thenReturn(List.of(n));
        when(alertRepository.existsByNodeIdAndLevelAndStatusNot(1L, "DEVICE", "RESOLVED"))
                .thenReturn(true);

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo("OFFLINE");
        verify(nodeRepository).save(n);
        verify(alertRepository, never()).save(any(Alert.class));
        verifyNoInteractions(messagingTemplate, notificationService);
    }

    @Test
    @DisplayName("Only the silent node is changed when several nodes are checked")
    void onlySilentNodeIsChanged() {
        SensorNode healthy = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(5));
        SensorNode silent = node(2L, "NODE-B1", "ONLINE", LocalDateTime.now().minusSeconds(300));
        when(nodeRepository.findAll()).thenReturn(List.of(healthy, silent));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        monitor.checkNodes();

        assertThat(healthy.getStatus()).isEqualTo("ONLINE");
        assertThat(silent.getStatus()).isEqualTo("OFFLINE");
        verify(nodeRepository).save(silent);
        verify(nodeRepository, never()).save(healthy);
    }

    @ParameterizedTest
    @CsvSource({"30,ONLINE", "59,ONLINE", "61,OFFLINE", "600,OFFLINE"})
    @DisplayName("The 60 second limit decides who goes offline")
    void limitDecidesStatus(long secondsSilent, String expectedStatus) {
        SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(secondsSilent));
        when(nodeRepository.findAll()).thenReturn(List.of(n));
        lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("With a 180 second limit, a node reporting every minute stays ONLINE")
    void minuteReportingNodeStaysOnlineWithLongLimit() {
        ReflectionTestUtils.setField(monitor, "offlineAfterSeconds", 180L);
        SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(100));
        when(nodeRepository.findAll()).thenReturn(List.of(n));

        monitor.checkNodes();

        assertThat(n.getStatus()).isEqualTo("ONLINE");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ── Battery monitoring tests ─────────────────────────────────────

    @Nested
    @DisplayName("Battery monitoring")
    class BatteryTests {

        @Test
        @DisplayName("Battery at 15% raises a MEDIUM alert")
        void lowBatteryRaisesMediumAlert() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now());
            n.setBatteryLevel(15);
            LocalDateTime now = LocalDateTime.now();

            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            monitor.checkBattery(n, now);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            Alert alert = captor.getValue();

            assertThat(alert.getTitle()).isEqualTo("Low battery on NODE-A1");
            assertThat(alert.getSeverity()).isEqualTo("MEDIUM");
            assertThat(alert.getMessage()).contains("15%").contains("Schedule replacement");
        }

        @Test
        @DisplayName("Battery at 5% raises a HIGH alert")
        void criticalBatteryRaisesHighAlert() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now());
            n.setBatteryLevel(5);
            LocalDateTime now = LocalDateTime.now();

            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            monitor.checkBattery(n, now);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            Alert alert = captor.getValue();

            assertThat(alert.getSeverity()).isEqualTo("HIGH");
            assertThat(alert.getMessage()).contains("5%").contains("Replace immediately");
        }

        @Test
        @DisplayName("Battery at 80% does not raise an alert")
        void normalBatteryNoAlert() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now());
            n.setBatteryLevel(80);

            monitor.checkBattery(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Null battery level is safely ignored")
        void nullBatteryIgnored() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now());
            n.setBatteryLevel(null);

            monitor.checkBattery(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Duplicate battery alert is not raised")
        void duplicateBatteryAlertNotRaised() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now());
            n.setBatteryLevel(8);

            when(alertRepository.existsByNodeIdAndLevelAndStatusNot(1L, "DEVICE", "RESOLVED"))
                    .thenReturn(true);

            monitor.checkBattery(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }
    }

    // ── Valve-stuck detection tests ──────────────────────────────────

    @Nested
    @DisplayName("Valve-stuck detection")
    class ValveStuckTests {

        @Test
        @DisplayName("A valve CLOSING for over 120s raises a CRITICAL alert")
        void stuckClosingRaisesAlert() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(300));
            n.setValveState("CLOSING");
            LocalDateTime now = LocalDateTime.now();

            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            monitor.checkValveStuck(n, now);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            Alert alert = captor.getValue();

            assertThat(alert.getTitle()).isEqualTo("Valve stuck closing on NODE-A1");
            assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
            assertThat(alert.getLevel()).isEqualTo("DEVICE");
            assertThat(alert.getMessage()).contains("CLOSING").contains("120 seconds");
        }

        @Test
        @DisplayName("A valve OPENING for less than 120s is not flagged")
        void recentOpeningNotFlagged() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(30));
            n.setValveState("OPENING");

            monitor.checkValveStuck(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("A settled valve (OPEN/CLOSED) is not flagged")
        void settledValveNotFlagged() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(300));
            n.setValveState("CLOSED");

            monitor.checkValveStuck(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Duplicate valve-stuck alert is not raised")
        void duplicateValveStuckAlertNotRaised() {
            SensorNode n = node(1L, "NODE-A1", "ONLINE", LocalDateTime.now().minusSeconds(300));
            n.setValveState("CLOSING");

            when(alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                    1L, "VALVE", "DEVICE", "RESOLVED")).thenReturn(true);

            monitor.checkValveStuck(n, LocalDateTime.now());

            verify(alertRepository, never()).save(any(Alert.class));
        }
    }
}
