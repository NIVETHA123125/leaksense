package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorHealthServiceTest {

    @Mock private ReadingRepository readingRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SensorHealthService service;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "stuckCount", 5);
        ReflectionTestUtils.setField(service, "methaneSpikePct", 100.0);
        ReflectionTestUtils.setField(service, "pressureSpikePct", 15.0);
        ReflectionTestUtils.setField(service, "oxygenSpikePct", 10.0);

        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 80, LocalDateTime.now(), zone);
    }

    private Reading reading(Long id, double methane, double pressure, double oxygen) {
        return new Reading(id, methane, pressure, oxygen, 25.0, LocalDateTime.now(), node);
    }

    @Nested
    @DisplayName("Spike detection")
    class SpikeTests {

        @Test
        @DisplayName("A large methane jump raises a SENSOR/HIGH alert")
        void largeMethaneSpikeRaisesAlert() {
            Reading previous = reading(1L, 100.0, 460.0, 20.9);
            Reading current = reading(2L, 350.0, 462.0, 20.8);

            when(readingRepository.findFirstByNodeIdOrderByRecordedAtDesc(1L))
                    .thenReturn(Optional.of(previous));
            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            service.checkSpike(node, current);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            Alert alert = captor.getValue();

            assertThat(alert.getTitle()).isEqualTo("Methane spike on NODE-A1");
            assertThat(alert.getLevel()).isEqualTo("SENSOR");
            assertThat(alert.getSeverity()).isEqualTo("HIGH");
            assertThat(alert.getMessage()).contains("jumped from 100.0 to 350.0 ppm");
            verify(messagingTemplate).convertAndSend(eq("/topic/alerts"), any(Alert.class));
            verify(notificationService).sendAlertEmail(alert);
        }

        @Test
        @DisplayName("A small methane change does not raise an alert")
        void smallMethaneChangeNoAlert() {
            Reading previous = reading(1L, 100.0, 460.0, 20.9);
            Reading current = reading(2L, 150.0, 462.0, 20.8);

            when(readingRepository.findFirstByNodeIdOrderByRecordedAtDesc(1L))
                    .thenReturn(Optional.of(previous));

            service.checkSpike(node, current);

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("A large pressure spike raises an alert")
        void largePressureSpikeRaisesAlert() {
            Reading previous = reading(1L, 100.0, 400.0, 20.9);
            Reading current = reading(2L, 105.0, 480.0, 20.8);

            when(readingRepository.findFirstByNodeIdOrderByRecordedAtDesc(1L))
                    .thenReturn(Optional.of(previous));
            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            service.checkSpike(node, current);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Pressure spike on NODE-A1");
        }

        @Test
        @DisplayName("No previous reading means no spike check")
        void noPreviousReadingNoSpike() {
            Reading current = reading(1L, 350.0, 462.0, 20.8);

            when(readingRepository.findFirstByNodeIdOrderByRecordedAtDesc(1L))
                    .thenReturn(Optional.empty());

            service.checkSpike(node, current);

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Duplicate alert is not raised if one is already open")
        void duplicateSpikeAlertNotRaised() {
            Reading previous = reading(1L, 100.0, 460.0, 20.9);
            Reading current = reading(2L, 350.0, 462.0, 20.8);

            when(readingRepository.findFirstByNodeIdOrderByRecordedAtDesc(1L))
                    .thenReturn(Optional.of(previous));
            when(alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                    1L, "METHANE", "SENSOR", "RESOLVED")).thenReturn(true);

            service.checkSpike(node, current);

            verify(alertRepository, never()).save(any(Alert.class));
        }
    }

    @Nested
    @DisplayName("Stuck sensor detection")
    class StuckTests {

        @Test
        @DisplayName("Five identical methane readings raise a FAULT/MEDIUM alert")
        void fiveIdenticalReadingsRaiseAlert() {
            List<Reading> readings = List.of(
                    reading(5L, 250.0, 460.0, 20.9),
                    reading(4L, 250.0, 461.0, 20.8),
                    reading(3L, 250.0, 459.0, 20.7),
                    reading(2L, 250.0, 458.0, 20.6),
                    reading(1L, 250.0, 462.0, 20.5));

            when(readingRepository.findByNodeIdOrderByRecordedAtDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(readings);
            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            service.checkStuck(node);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            Alert alert = captor.getValue();

            assertThat(alert.getTitle()).isEqualTo("Methane sensor may be stuck on NODE-A1");
            assertThat(alert.getLevel()).isEqualTo("FAULT");
            assertThat(alert.getSeverity()).isEqualTo("MEDIUM");
            assertThat(alert.getMessage()).contains("250.0 ppm").contains("5 consecutive");
        }

        @Test
        @DisplayName("Varying readings do not trigger stuck detection")
        void varyingReadingsNoAlert() {
            List<Reading> readings = List.of(
                    reading(5L, 250.0, 460.0, 20.9),
                    reading(4L, 255.0, 462.0, 20.8),
                    reading(3L, 248.0, 458.0, 20.7),
                    reading(2L, 250.0, 461.0, 20.9),
                    reading(1L, 253.0, 459.0, 20.8));

            when(readingRepository.findByNodeIdOrderByRecordedAtDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(readings);

            service.checkStuck(node);

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Fewer than N readings means no stuck check")
        void fewerThanNReadingsNoCheck() {
            List<Reading> readings = List.of(
                    reading(2L, 250.0, 460.0, 20.9),
                    reading(1L, 250.0, 460.0, 20.9));

            when(readingRepository.findByNodeIdOrderByRecordedAtDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(readings);

            service.checkStuck(node);

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("All-identical pressure also triggers stuck detection")
        void identicalPressureTriggersStuck() {
            List<Reading> readings = List.of(
                    reading(5L, 100.0, 460.0, 20.9),
                    reading(4L, 105.0, 460.0, 20.8),
                    reading(3L, 110.0, 460.0, 20.7),
                    reading(2L, 95.0, 460.0, 20.6),
                    reading(1L, 102.0, 460.0, 20.5));

            when(readingRepository.findByNodeIdOrderByRecordedAtDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(readings);
            when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

            service.checkStuck(node);

            ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Pressure sensor may be stuck on NODE-A1");
        }

        @Test
        @DisplayName("Duplicate stuck alert is not raised")
        void duplicateStuckAlertNotRaised() {
            List<Reading> readings = List.of(
                    reading(5L, 250.0, 460.0, 20.9),
                    reading(4L, 250.0, 461.0, 20.8),
                    reading(3L, 250.0, 459.0, 20.7),
                    reading(2L, 250.0, 458.0, 20.6),
                    reading(1L, 250.0, 462.0, 20.5));

            when(readingRepository.findByNodeIdOrderByRecordedAtDesc(
                    eq(1L), any(PageRequest.class))).thenReturn(readings);
            when(alertRepository.existsByNodeIdAndSensorAndLevelAndStatusNot(
                    1L, "METHANE", "FAULT", "RESOLVED")).thenReturn(true);

            service.checkStuck(node);

            verify(alertRepository, never()).save(any(Alert.class));
        }
    }
}
