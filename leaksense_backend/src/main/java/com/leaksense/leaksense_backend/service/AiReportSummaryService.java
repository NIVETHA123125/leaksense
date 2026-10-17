package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.AiReportSummary;
import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportSummaryService {

    private final AlertRepository alertRepository;
    private final AlarmEventRepository alarmRepository;
    private final ValveActionRepository valveActionRepository;
    private final SensorNodeRepository nodeRepository;
    private final ZoneRepository zoneRepository;

    public AiReportSummary generateSummary() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<Alert> weeklyAlerts = alertRepository.findByCreatedAtAfter(sevenDaysAgo);
        List<AlarmEvent> weeklyAlarms = alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                sevenDaysAgo, LocalDateTime.now());
        List<ValveAction> weeklyValves = valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(
                sevenDaysAgo, LocalDateTime.now());
        List<SensorNode> allNodes = nodeRepository.findAll();
        List<Zone> allZones = zoneRepository.findAll();

        long criticalAlerts = weeklyAlerts.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity())).count();
        long warningAlerts = weeklyAlerts.stream().filter(a -> "WARNING".equalsIgnoreCase(a.getSeverity())).count();
        long closeActions = weeklyValves.stream().filter(v -> "CLOSE".equalsIgnoreCase(v.getAction())).count();
        long offlineNodes = allNodes.stream().filter(n -> "OFFLINE".equalsIgnoreCase(n.getStatus())).count();
        long lowBatteryNodes = allNodes.stream().filter(n -> n.getBatteryLevel() != null && n.getBatteryLevel() <= 20).count();

        // Safety score calculation: 100 minus penalties
        double score = 100.0;
        score -= (criticalAlerts * 15.0);
        score -= (weeklyAlarms.size() * 10.0);
        score -= (warningAlerts * 3.0);
        score -= (offlineNodes * 12.0);
        score -= (lowBatteryNodes * 4.0);
        score = Math.max(0.0, Math.min(100.0, score));

        String riskLevel;
        if (score >= 90.0) riskLevel = "OPTIMAL";
        else if (score >= 75.0) riskLevel = "LOW_RISK";
        else if (score >= 50.0) riskLevel = "ELEVATED_RISK";
        else riskLevel = "CRITICAL_HAZARD";

        List<String> observations = new ArrayList<>();
        observations.add(String.format("Recorded %d total alert(s) across %d monitoring zones in the past 7 days.",
                weeklyAlerts.size(), allZones.size()));

        if (criticalAlerts > 0) {
            observations.add(String.format("%d CRITICAL leak event(s) detected requiring high-priority remediation.", criticalAlerts));
        } else {
            observations.add("Zero critical threshold breaches were detected during this reporting cycle.");
        }

        if (!weeklyAlarms.isEmpty()) {
            observations.add(String.format("%d acoustic/visual alarm sirens were engaged.", weeklyAlarms.size()));
        }

        if (closeActions > 0) {
            observations.add(String.format("%d emergency valve shutdown(s) were successfully executed to isolate hazard zones.", closeActions));
        }

        if (offlineNodes > 0) {
            observations.add(String.format("%d sensor node(s) currently OFFLINE and not sending telemetry.", offlineNodes));
        }

        if (lowBatteryNodes > 0) {
            observations.add(String.format("%d sensor node(s) have low battery levels (≤20%%).", lowBatteryNodes));
        }

        List<String> recommendations = new ArrayList<>();
        if (criticalAlerts > 0) {
            recommendations.add("Conduct on-site ultrasonic inspection and leak sniffing on zones with critical alerts.");
        }
        if (offlineNodes > 0) {
            recommendations.add("Check wireless gateways and power supplies for offline sensor nodes.");
        }
        if (lowBatteryNodes > 0) {
            recommendations.add("Schedule technician battery replacements for nodes with battery ≤ 20%.");
        }
        if (closeActions > 0) {
            recommendations.add("Inspect valve mechanical integrity and seal tightness before re-commissioning isolated lines.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Continue scheduled weekly monitoring rounds. All sensor readings and battery levels are within optimal parameters.");
        }

        String narrative = String.format(
                "Executive AI Pipeline Health Assessment: The monitored infrastructure maintains an overall safety rating of %.1f/100 (%s). Over the last 7 days, %d alerts and %d alarm events were evaluated across %d active nodes. %s",
                score, riskLevel, weeklyAlerts.size(), weeklyAlarms.size(), allNodes.size(),
                criticalAlerts > 0
                        ? "Immediate attention is required to resolve active leak conditions."
                        : "Operations remain stable with no major hazard indicators.");

        return AiReportSummary.builder()
                .executiveSummary(narrative)
                .riskLevel(riskLevel)
                .overallSafetyScore(Math.round(score * 10.0) / 10.0)
                .keyObservations(observations)
                .recommendedActions(recommendations)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
