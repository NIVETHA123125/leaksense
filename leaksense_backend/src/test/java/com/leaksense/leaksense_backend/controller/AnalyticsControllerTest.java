package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.*;
import com.leaksense.leaksense_backend.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController controller;

    @Test
    @DisplayName("getSensorTrends calls service and returns trend points")
    void getSensorTrends() {
        SensorTrendPoint point = SensorTrendPoint.builder()
                .id(1L)
                .nodeCode("NODE-A1")
                .methanePpm(0.5)
                .pressureKpa(101.2)
                .oxygenPercent(20.9)
                .riskScore(10.0)
                .recordedAt(LocalDateTime.now())
                .build();
        when(analyticsService.getSensorTrends(1L, 24)).thenReturn(List.of(point));

        List<SensorTrendPoint> result = controller.getSensorTrends(1L, 24);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNodeCode()).isEqualTo("NODE-A1");
        verify(analyticsService).getSensorTrends(1L, 24);
    }

    @Test
    @DisplayName("getRiskDistribution calls service and returns distribution")
    void getRiskDistribution() {
        RiskDistributionDto dto = RiskDistributionDto.builder()
                .zoneRiskCounts(Map.of("SAFE", 2L))
                .readingRiskCounts(Map.of("SAFE", 10L))
                .averageRiskScore(15.0)
                .overallSystemStatus("OPTIMAL")
                .build();
        when(analyticsService.getRiskDistribution()).thenReturn(dto);

        RiskDistributionDto result = controller.getRiskDistribution();

        assertThat(result.getOverallSystemStatus()).isEqualTo("OPTIMAL");
        verify(analyticsService).getRiskDistribution();
    }

    @Test
    @DisplayName("getAlertTimeline calls service with days parameter")
    void getAlertTimeline() {
        AlertTimelinePoint point = AlertTimelinePoint.builder()
                .date("2026-10-15")
                .criticalCount(1)
                .warningCount(2)
                .totalCount(3)
                .build();
        when(analyticsService.getAlertTimeline(7)).thenReturn(List.of(point));

        List<AlertTimelinePoint> result = controller.getAlertTimeline(7);

        assertThat(result).hasSize(1);
        verify(analyticsService).getAlertTimeline(7);
    }

    @Test
    @DisplayName("getAlertsBySensor calls service and returns distribution")
    void getAlertsBySensor() {
        AlertCategoryDistribution cat = AlertCategoryDistribution.builder()
                .sensor("methane")
                .count(5)
                .percentage(50.0)
                .build();
        when(analyticsService.getAlertsBySensor()).thenReturn(List.of(cat));

        List<AlertCategoryDistribution> result = controller.getAlertsBySensor();

        assertThat(result).hasSize(1);
        verify(analyticsService).getAlertsBySensor();
    }

    @Test
    @DisplayName("getValveActivityStats calls service and returns stats")
    void getValveActivityStats() {
        ValveActivityStats stats = ValveActivityStats.builder()
                .totalActions(10)
                .closeCount(5)
                .openCount(5)
                .build();
        when(analyticsService.getValveActivityStats()).thenReturn(stats);

        ValveActivityStats result = controller.getValveActivityStats();

        assertThat(result.getTotalActions()).isEqualTo(10);
        verify(analyticsService).getValveActivityStats();
    }
}
