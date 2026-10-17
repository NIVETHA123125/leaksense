package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService alertService;

    private Zone zone;
    private SensorNode node;
    private Alert alert;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
        alert = new Alert(10L, "Methane Alert", "Methane high", "SENSOR", "CRITICAL",
                "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");
    }

    @Nested
    @DisplayName("Active & Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("getActiveAlerts returns active non-resolved alerts")
        void getActiveAlerts() {
            when(alertRepository.findByStatusNotOrderByCreatedAtDesc("RESOLVED"))
                    .thenReturn(List.of(alert));

            List<Alert> result = alertService.getActiveAlerts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
            verify(alertRepository).findByStatusNotOrderByCreatedAtDesc("RESOLVED");
        }

        @Test
        @DisplayName("acknowledge sets status and username")
        void acknowledgeAlert() {
            when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

            Alert updated = alertService.acknowledge(10L, "operator1");

            assertThat(updated.getStatus()).isEqualTo("ACKNOWLEDGED");
            assertThat(updated.getAcknowledgedBy()).isEqualTo("operator1");
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("resolve sets status and resolvedAt timestamp")
        void resolveAlert() {
            when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

            Alert updated = alertService.resolve(10L);

            assertThat(updated.getStatus()).isEqualTo("RESOLVED");
            assertThat(updated.getResolvedAt()).isNotNull();
            verify(alertRepository).save(alert);
        }

        @Test
        @DisplayName("Throws exception when acknowledging non-existent alert")
        void acknowledgeNotFound() {
            when(alertRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.acknowledge(999L, "user"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Alert not found");
        }

        @Test
        @DisplayName("Throws exception when resolving non-existent alert")
        void resolveNotFound() {
            when(alertRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.resolve(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Alert not found");
        }
    }

    @Nested
    @DisplayName("Alert History Filter Tests")
    class HistoryFilterTests {

        @Test
        @DisplayName("getAlertHistory queries repository with specification and pageable")
        void getAlertHistoryFilters() {
            Page<Alert> page = new PageImpl<>(List.of(alert));
            when(alertRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Alert> result = alertService.getAlertHistory(
                    1L, 1L, "methane", "SENSOR", "CRITICAL", "OPEN",
                    LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Methane Alert");

            ArgumentCaptor<Specification<Alert>> specCaptor = ArgumentCaptor.forClass(Specification.class);
            verify(alertRepository).findAll(specCaptor.capture(), eq(pageable));
            assertThat(specCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("getAlertHistory handles all null filters gracefully")
        void getAlertHistoryAllNulls() {
            Page<Alert> page = new PageImpl<>(List.of(alert));
            when(alertRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Alert> result = alertService.getAlertHistory(
                    null, null, null, null, null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(alertRepository).findAll(any(Specification.class), eq(pageable));
        }
    }
}
