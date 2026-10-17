package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.AiInsight;
import com.leaksense.leaksense_backend.dto.EarlyWarningResult;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final SensorNodeRepository nodeRepository;
    private final AiEarlyWarningService earlyWarningService;

    public List<AiInsight> getInsights() {
        List<AiInsight> insights = new ArrayList<>();
        List<SensorNode> nodes = nodeRepository.findAll();

        for (SensorNode node : nodes) {
            String zoneName = node.getZone() != null ? node.getZone().getName() : "Unassigned Zone";

            // 1. Early-warning trajectory scan
            Optional<EarlyWarningResult> warning = earlyWarningService.scanNode(node);
            warning.ifPresent(w -> insights.add(AiInsight.builder()
                    .id("EW-" + node.getNodeCode())
                    .zoneName(zoneName)
                    .nodeCode(node.getNodeCode())
                    .severity("HIGH".equals(w.getLevel()) ? "CRITICAL" : "WARNING")
                    .type("EARLY_WARNING")
                    .title("Predictive Leak Risk: " + w.getPattern())
                    .description(w.getDescription())
                    .confidenceScore(w.getConfidence())
                    .recommendation(w.getRecommendation())
                    .detectedAt(LocalDateTime.now())
                    .build()));

            // 2. Battery health insight
            if (node.getBatteryLevel() != null && node.getBatteryLevel() <= 15) {
                insights.add(AiInsight.builder()
                        .id("BAT-" + node.getNodeCode())
                        .zoneName(zoneName)
                        .nodeCode(node.getNodeCode())
                        .severity(node.getBatteryLevel() <= 10 ? "CRITICAL" : "WARNING")
                        .type("MAINTENANCE")
                        .title("Critical Battery Depletion")
                        .description(String.format("Node battery is at %d%%, risking telemetry drop and sensor blindness.", node.getBatteryLevel()))
                        .confidenceScore(0.99)
                        .recommendation("Swap battery pack on next field inspection pass.")
                        .detectedAt(LocalDateTime.now())
                        .build());
            }

            // 3. Offline telemetry insight
            if ("OFFLINE".equalsIgnoreCase(node.getStatus())) {
                insights.add(AiInsight.builder()
                        .id("OFF-" + node.getNodeCode())
                        .zoneName(zoneName)
                        .nodeCode(node.getNodeCode())
                        .severity("CRITICAL")
                        .type("ANOMALY")
                        .title("Sensor Node Heartbeat Lost")
                        .description("Node has stopped broadcasting telemetry readings to the gateway.")
                        .confidenceScore(1.0)
                        .recommendation("Inspect wireless gateway and power supply immediately.")
                        .detectedAt(LocalDateTime.now())
                        .build());
            }

            // 4. Valve state fault insight
            if ("FAULT".equalsIgnoreCase(node.getValveState())) {
                insights.add(AiInsight.builder()
                        .id("VLV-" + node.getNodeCode())
                        .zoneName(zoneName)
                        .nodeCode(node.getNodeCode())
                        .severity("CRITICAL")
                        .type("ANOMALY")
                        .title("Valve Actuator Mechanical Fault")
                        .description("Actuator timed out or reported mechanical stall during travel.")
                        .confidenceScore(0.95)
                        .recommendation("Manually verify valve stem position and test solenoid voltage.")
                        .detectedAt(LocalDateTime.now())
                        .build());
            }
        }

        if (insights.isEmpty()) {
            insights.add(AiInsight.builder()
                    .id("SYS-OPTIMAL")
                    .zoneName("System-Wide")
                    .nodeCode("ALL")
                    .severity("INFO")
                    .type("PREDICTION")
                    .title("All Monitored Parameters Nominal")
                    .description("AI predictive scanning detected no anomalies, cross-sensor drifts, or early leak signatures.")
                    .confidenceScore(0.98)
                    .recommendation("Maintain normal operational schedule.")
                    .detectedAt(LocalDateTime.now())
                    .build());
        }

        return insights;
    }
}
