package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.AiReportSummary;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiReportSummaryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlarmEventRepository alarmRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private AiReportSummaryService summaryService;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
    }

    @Test
    @DisplayName("Calculates optimal safety score and summary when no incidents exist")
    void generatesOptimalSummary() {
        when(alertRepository.findByCreatedAtAfter(any())).thenReturn(Collections.emptyList());
        when(alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(zoneRepository.findAll()).thenReturn(List.of(zone));

        AiReportSummary summary = summaryService.generateSummary();

        assertThat(summary.getOverallSafetyScore()).isEqualTo(100.0);
        assertThat(summary.getRiskLevel()).isEqualTo("OPTIMAL");
        assertThat(summary.getExecutiveSummary()).contains("100.0/100 (OPTIMAL)");
        assertThat(summary.getKeyObservations()).isNotEmpty();
        assertThat(summary.getRecommendedActions()).isNotEmpty();
    }

    @Test
    @DisplayName("Computes safety penalty when critical alerts and offline nodes exist")
    void computesSafetyPenalty() {
        Alert criticalAlert = new Alert(1L, "Methane Critical", "High gas", "SENSOR", "CRITICAL",
                "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");
        SensorNode offlineNode = new SensorNode(2L, "NODE-B1", "OFFLINE", 10, LocalDateTime.now(), zone, "OPEN");

        when(alertRepository.findByCreatedAtAfter(any())).thenReturn(List.of(criticalAlert));
        when(alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(any(), any())).thenReturn(Collections.emptyList());
        when(nodeRepository.findAll()).thenReturn(List.of(node, offlineNode));
        when(zoneRepository.findAll()).thenReturn(List.of(zone));

        AiReportSummary summary = summaryService.generateSummary();

        // 100 - (15 critical) - (12 offline) - (4 low battery) = 69.0
        assertThat(summary.getOverallSafetyScore()).isEqualTo(69.0);
        assertThat(summary.getRiskLevel()).isEqualTo("ELEVATED_RISK");
        assertThat(summary.getKeyObservations().stream().anyMatch(o -> o.contains("CRITICAL"))).isTrue();
        assertThat(summary.getRecommendedActions().stream().anyMatch(r -> r.contains("ultrasonic inspection"))).isTrue();
    }
}
