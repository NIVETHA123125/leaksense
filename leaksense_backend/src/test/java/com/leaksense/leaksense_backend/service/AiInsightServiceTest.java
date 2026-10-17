package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.AiInsight;
import com.leaksense.leaksense_backend.dto.EarlyWarningResult;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInsightServiceTest {

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private AiEarlyWarningService earlyWarningService;

    @InjectMocks
    private AiInsightService insightService;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
    }

    @Test
    @DisplayName("Generates insights when early-warning, low battery, and offline conditions occur")
    void generatesInsightsOnConditions() {
        SensorNode riskyNode = new SensorNode(2L, "NODE-B1", "OFFLINE", 12, LocalDateTime.now(), zone, "FAULT");
        when(nodeRepository.findAll()).thenReturn(List.of(node, riskyNode));

        EarlyWarningResult warning = EarlyWarningResult.builder()
                .warning(true)
                .level("HIGH")
                .confidence(0.88)
                .pattern("PRESSURE_DECAY_WITH_GAS_CREEP")
                .description("Pressure drop with rising methane")
                .recommendation("Inspect junction")
                .build();
        when(earlyWarningService.scanNode(node)).thenReturn(Optional.of(warning));
        when(earlyWarningService.scanNode(riskyNode)).thenReturn(Optional.empty());

        List<AiInsight> insights = insightService.getInsights();

        assertThat(insights).isNotEmpty();
        // Should contain EW for node, BAT for riskyNode, OFF for riskyNode, VLV for riskyNode
        assertThat(insights.stream().anyMatch(i -> i.getType().equals("EARLY_WARNING"))).isTrue();
        assertThat(insights.stream().anyMatch(i -> i.getType().equals("MAINTENANCE"))).isTrue();
        assertThat(insights.stream().anyMatch(i -> i.getTitle().contains("Heartbeat Lost"))).isTrue();
        assertThat(insights.stream().anyMatch(i -> i.getTitle().contains("Mechanical Fault"))).isTrue();
    }

    @Test
    @DisplayName("Returns nominal system insight when all nodes are healthy")
    void returnsNominalWhenAllHealthy() {
        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(earlyWarningService.scanNode(any())).thenReturn(Optional.empty());

        List<AiInsight> insights = insightService.getInsights();

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).getId()).isEqualTo("SYS-OPTIMAL");
        assertThat(insights.get(0).getSeverity()).isEqualTo("INFO");
        assertThat(insights.get(0).getTitle()).contains("Nominal");
    }
}
