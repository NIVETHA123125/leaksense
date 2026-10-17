package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.*;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final ValveActionRepository valveActionRepository;
    private final ZoneRepository zoneRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<SensorTrendPoint> getSensorTrends(Long nodeId, int hours) {
        int windowHours = hours > 0 ? Math.min(hours, 720) : 24; // capped at 30 days
        LocalDateTime cutoff = LocalDateTime.now().minusHours(windowHours);

        List<Reading> readings = (nodeId != null)
                ? readingRepository.findByNodeIdAndRecordedAtAfterOrderByRecordedAtAsc(nodeId, cutoff)
                : readingRepository.findByRecordedAtAfterOrderByRecordedAtAsc(cutoff);

        // Fallback: if no readings exist within the specific cutoff timeframe,
        // retrieve the most recent recorded telemetry so the charts and telemetry
        // trends never show a blank empty screen when historical data exists.
        if (readings.isEmpty()) {
            List<Reading> recent = (nodeId != null)
                    ? readingRepository.findTop100ByNodeIdOrderByRecordedAtDesc(nodeId)
                    : readingRepository.findTop100ByOrderByRecordedAtDesc();
            if (!recent.isEmpty()) {
                readings = recent.stream()
                        .sorted(Comparator.comparing(Reading::getRecordedAt))
                        .toList();
            }
        }

        return readings.stream()
                .map(r -> SensorTrendPoint.builder()
                        .id(r.getId())
                        .nodeCode(r.getNode() != null ? r.getNode().getNodeCode() : "-")
                        .methanePpm(r.getMethanePpm())
                        .pressureKpa(r.getPressureKpa())
                        .oxygenPercent(r.getOxygenPercent())
                        .riskScore(r.getRiskScore())
                        .recordedAt(r.getRecordedAt())
                        .build())
                .toList();
    }

    public RiskDistributionDto getRiskDistribution() {
        List<Zone> zones = zoneRepository.findAll();
        Map<String, Long> zoneRiskCounts = new HashMap<>();
        for (String level : List.of("SAFE", "LOW", "MODERATE", "HIGH", "CRITICAL")) {
            zoneRiskCounts.put(level, 0L);
        }
        for (Zone zone : zones) {
            String level = zone.getRiskLevel() != null ? zone.getRiskLevel().toUpperCase() : "SAFE";
            zoneRiskCounts.put(level, zoneRiskCounts.getOrDefault(level, 0L) + 1L);
        }

        List<Reading> recentReadings = readingRepository.findTop50ByOrderByRecordedAtDesc();
        Map<String, Long> readingRiskCounts = new HashMap<>();
        for (String level : List.of("SAFE", "LOW", "MODERATE", "HIGH", "CRITICAL")) {
            readingRiskCounts.put(level, 0L);
        }

        double totalRisk = 0.0;
        for (Reading r : recentReadings) {
            totalRisk += r.getRiskScore();
            String bucket = categorizeRiskScore(r.getRiskScore());
            readingRiskCounts.put(bucket, readingRiskCounts.getOrDefault(bucket, 0L) + 1L);
        }

        double avgRisk = recentReadings.isEmpty() ? 0.0 : Math.round((totalRisk / recentReadings.size()) * 10.0) / 10.0;

        String systemStatus = "OPTIMAL";
        if (zoneRiskCounts.get("CRITICAL") > 0 || readingRiskCounts.get("CRITICAL") > 0) {
            systemStatus = "CRITICAL";
        } else if (zoneRiskCounts.get("HIGH") > 0 || readingRiskCounts.get("HIGH") > 0
                || zoneRiskCounts.get("MODERATE") > 0) {
            systemStatus = "ATTENTION_REQUIRED";
        }

        return RiskDistributionDto.builder()
                .zoneRiskCounts(zoneRiskCounts)
                .readingRiskCounts(readingRiskCounts)
                .averageRiskScore(avgRisk)
                .overallSystemStatus(systemStatus)
                .build();
    }

    public List<AlertTimelinePoint> getAlertTimeline(int days) {
        int windowDays = days > 0 ? Math.min(days, 90) : 7;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(windowDays - 1);

        List<Alert> alerts = alertRepository.findByCreatedAtAfter(startDate.atStartOfDay());

        Map<String, List<Alert>> byDate = alerts.stream()
                .filter(a -> a.getCreatedAt() != null)
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate().format(DATE_FORMATTER)));

        List<AlertTimelinePoint> timeline = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            String dateStr = d.format(DATE_FORMATTER);
            List<Alert> dayAlerts = byDate.getOrDefault(dateStr, Collections.emptyList());

            long critical = dayAlerts.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity())).count();
            long warning = dayAlerts.stream().filter(a -> "WARNING".equalsIgnoreCase(a.getSeverity())).count();
            long info = dayAlerts.size() - critical - warning;

            timeline.add(AlertTimelinePoint.builder()
                    .date(dateStr)
                    .criticalCount(critical)
                    .warningCount(warning)
                    .infoCount(info)
                    .totalCount(dayAlerts.size())
                    .build());
        }

        return timeline;
    }

    public List<AlertCategoryDistribution> getAlertsBySensor() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Alert> alerts = alertRepository.findByCreatedAtAfter(cutoff);
        if (alerts.isEmpty()) {
            alerts = alertRepository.findAll();
        }

        if (alerts.isEmpty()) {
            return Collections.emptyList();
        }

        long total = alerts.size();
        Map<String, Long> countMap = new HashMap<>();
        for (Alert alert : alerts) {
            String key = (alert.getSensor() != null && !alert.getSensor().isBlank())
                    ? alert.getSensor().toLowerCase()
                    : (alert.getLevel() != null ? alert.getLevel().toLowerCase() : "system");
            countMap.put(key, countMap.getOrDefault(key, 0L) + 1L);
        }

        return countMap.entrySet().stream()
                .map(e -> AlertCategoryDistribution.builder()
                        .sensor(e.getKey())
                        .count(e.getValue())
                        .percentage(Math.round(((double) e.getValue() / total) * 1000.0) / 10.0)
                        .build())
                .sorted(Comparator.comparingLong(AlertCategoryDistribution::getCount).reversed())
                .toList();
    }

    public ValveActivityStats getValveActivityStats() {
        List<ValveAction> actions = valveActionRepository.findAll();

        long total = actions.size();
        long closeCount = actions.stream().filter(a -> "CLOSE".equalsIgnoreCase(a.getAction())).count();
        long openCount = actions.stream().filter(a -> "OPEN".equalsIgnoreCase(a.getAction())).count();
        long autoCount = actions.stream().filter(a -> "SYSTEM".equalsIgnoreCase(a.getTriggeredBy())).count();
        long manualCount = total - autoCount;

        List<ValveActivityStats.ValveActionSummary> recent = valveActionRepository.findTop10ByOrderByPerformedAtDesc()
                .stream()
                .map(a -> ValveActivityStats.ValveActionSummary.builder()
                        .id(a.getId())
                        .action(a.getAction())
                        .zoneName(a.getZone() != null ? a.getZone().getName() : "-")
                        .triggeredBy(a.getTriggeredBy())
                        .reason(a.getReason())
                        .performedAt(a.getPerformedAt())
                        .build())
                .toList();

        return ValveActivityStats.builder()
                .totalActions(total)
                .closeCount(closeCount)
                .openCount(openCount)
                .manualCount(manualCount)
                .autoCount(autoCount)
                .recentActions(recent)
                .build();
    }

    private String categorizeRiskScore(double score) {
        if (score >= 85.0) return "CRITICAL";
        if (score >= 60.0) return "HIGH";
        if (score >= 35.0) return "MODERATE";
        if (score >= 15.0) return "LOW";
        return "SAFE";
    }
}
