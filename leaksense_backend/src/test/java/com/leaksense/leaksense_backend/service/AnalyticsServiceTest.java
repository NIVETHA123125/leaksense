package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.*;
import com.leaksense.leaksense_backend.entity.*;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ReadingRepository readingRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Zone zone;
    private SensorNode node;
    private Reading reading;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 85, LocalDateTime.now(), zone, "OPEN");
        reading = new Reading(100L, 0.4, 101.3, 20.9, 12.5, LocalDateTime.now(), node);
    }

    @Nested
    @DisplayName("Sensor Trends Tests")
    class SensorTrendsTests {

        @Test
        @DisplayName("Returns sensor trends for specific node")
        void getSensorTrendsByNode() {
            when(readingRepository.findByNodeIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(reading));

            List<SensorTrendPoint> points = analyticsService.getSensorTrends(1L, 12);

            assertThat(points).hasSize(1);
            assertThat(points.get(0).getNodeCode()).isEqualTo("NODE-A1");
            assertThat(points.get(0).getMethanePpm()).isEqualTo(0.4);
            assertThat(points.get(0).getPressureKpa()).isEqualTo(101.3);
            assertThat(points.get(0).getOxygenPercent()).isEqualTo(20.9);
            assertThat(points.get(0).getRiskScore()).isEqualTo(12.5);

            verify(readingRepository).findByNodeIdAndRecordedAtAfterOrderByRecordedAtAsc(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Returns sensor trends across all nodes when nodeId is null")
        void getSensorTrendsAllNodes() {
            when(readingRepository.findByRecordedAtAfterOrderByRecordedAtAsc(any(LocalDateTime.class)))
                    .thenReturn(List.of(reading));

            List<SensorTrendPoint> points = analyticsService.getSensorTrends(null, 24);

            assertThat(points).hasSize(1);
            verify(readingRepository).findByRecordedAtAfterOrderByRecordedAtAsc(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Risk Distribution Tests")
    class RiskDistributionTests {

        @Test
        @DisplayName("Calculates risk distribution correctly across zones and readings")
        void getRiskDistributionCalculations() {
            Zone safeZone = new Zone(1L, "Zone A", "Safe", "SAFE");
            Zone criticalZone = new Zone(2L, "Zone B", "Danger", "CRITICAL");
            when(zoneRepository.findAll()).thenReturn(List.of(safeZone, criticalZone));

            Reading lowRisk = new Reading(1L, 0.2, 101.0, 20.9, 10.0, LocalDateTime.now(), node);
            Reading highRisk = new Reading(2L, 2.5, 95.0, 19.0, 75.0, LocalDateTime.now(), node);
            when(readingRepository.findTop50ByOrderByRecordedAtDesc()).thenReturn(List.of(lowRisk, highRisk));

            RiskDistributionDto dto = analyticsService.getRiskDistribution();

            assertThat(dto).isNotNull();
            assertThat(dto.getZoneRiskCounts()).containsEntry("SAFE", 1L);
            assertThat(dto.getZoneRiskCounts()).containsEntry("CRITICAL", 1L);
            assertThat(dto.getReadingRiskCounts()).containsEntry("SAFE", 1L);
            assertThat(dto.getReadingRiskCounts()).containsEntry("HIGH", 1L);
            assertThat(dto.getAverageRiskScore()).isEqualTo(42.5);
            assertThat(dto.getOverallSystemStatus()).isEqualTo("CRITICAL");
        }
    }

    @Nested
    @DisplayName("Alert Timeline Tests")
    class AlertTimelineTests {

        @Test
        @DisplayName("Constructs daily alert timeline correctly")
        void getAlertTimelineDaily() {
            LocalDateTime now = LocalDateTime.now();
            Alert criticalAlert = new Alert(1L, "Methane", "High", "SENSOR", "CRITICAL",
                    "OPEN", null, now, null, zone, node, "methane");
            Alert warningAlert = new Alert(2L, "Pressure", "Low", "SENSOR", "WARNING",
                    "OPEN", null, now, null, zone, node, "pressure");

            when(alertRepository.findByCreatedAtAfter(any(LocalDateTime.class)))
                    .thenReturn(List.of(criticalAlert, warningAlert));

            List<AlertTimelinePoint> timeline = analyticsService.getAlertTimeline(7);

            assertThat(timeline).hasSize(7);
            AlertTimelinePoint todayPoint = timeline.get(timeline.size() - 1);
            assertThat(todayPoint.getCriticalCount()).isEqualTo(1L);
            assertThat(todayPoint.getWarningCount()).isEqualTo(1L);
            assertThat(todayPoint.getTotalCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Alerts By Sensor Tests")
    class AlertsBySensorTests {

        @Test
        @DisplayName("Groups alerts by sensor and computes percentages")
        void getAlertsBySensorDistribution() {
            LocalDateTime now = LocalDateTime.now();
            Alert a1 = new Alert(1L, "M1", "Methane", "SENSOR", "CRITICAL", "OPEN", null, now, null, zone, node, "methane");
            Alert a2 = new Alert(2L, "M2", "Methane", "SENSOR", "WARNING", "OPEN", null, now, null, zone, node, "methane");
            Alert a3 = new Alert(3L, "P1", "Pressure", "SENSOR", "WARNING", "OPEN", null, now, null, zone, node, "pressure");

            when(alertRepository.findByCreatedAtAfter(any(LocalDateTime.class)))
                    .thenReturn(List.of(a1, a2, a3));

            List<AlertCategoryDistribution> list = analyticsService.getAlertsBySensor();

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getSensor()).isEqualTo("methane");
            assertThat(list.get(0).getCount()).isEqualTo(2L);
            assertThat(list.get(0).getPercentage()).isEqualTo(66.7);

            assertThat(list.get(1).getSensor()).isEqualTo("pressure");
            assertThat(list.get(1).getCount()).isEqualTo(1L);
            assertThat(list.get(1).getPercentage()).isEqualTo(33.3);
        }
    }

    @Nested
    @DisplayName("Valve Activity Stats Tests")
    class ValveActivityStatsTests {

        @Test
        @DisplayName("Aggregates valve statistics and recent action summaries")
        void getValveActivityStats() {
            LocalDateTime now = LocalDateTime.now();
            ValveAction a1 = new ValveAction(1L, "CLOSE", "admin", "Emergency", "127.0.0.1", now, zone);
            ValveAction a2 = new ValveAction(2L, "OPEN", "operator", "Testing", "127.0.0.1", now, zone);
            ValveAction a3 = new ValveAction(3L, "CLOSE", "SYSTEM", "Auto-isolate", "127.0.0.1", now, zone);

            when(valveActionRepository.findAll()).thenReturn(List.of(a1, a2, a3));
            when(valveActionRepository.findTop10ByOrderByPerformedAtDesc()).thenReturn(List.of(a1, a2, a3));

            ValveActivityStats stats = analyticsService.getValveActivityStats();

            assertThat(stats.getTotalActions()).isEqualTo(3L);
            assertThat(stats.getCloseCount()).isEqualTo(2L);
            assertThat(stats.getOpenCount()).isEqualTo(1L);
            assertThat(stats.getAutoCount()).isEqualTo(1L);
            assertThat(stats.getManualCount()).isEqualTo(2L);
            assertThat(stats.getRecentActions()).hasSize(3);
            assertThat(stats.getRecentActions().get(0).getZoneName()).isEqualTo("Zone A");
        }
    }
}
