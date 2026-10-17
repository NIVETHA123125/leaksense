package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.ChatRequest;
import com.leaksense.leaksense_backend.dto.ChatResponse;
import com.leaksense.leaksense_backend.entity.*;
import com.leaksense.leaksense_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AlertRepository alertRepository;
    private final AlarmEventRepository alarmRepository;
    private final SensorNodeRepository nodeRepository;
    private final ZoneRepository zoneRepository;
    private final ValveActionRepository valveActionRepository;
    private final ReadingRepository readingRepository;

    @Value("${leaksense.ai.api-key:}")
    private String apiKey;

    public ChatResponse processMessage(ChatRequest request) {
        String msg = (request != null && request.getMessage() != null)
                ? request.getMessage().trim()
                : "";

        if (msg.isBlank()) {
            return ChatResponse.builder()
                    .reply("Hello! I am your LeakSense Safety AI Assistant. You can ask me about pipeline risk, active alerts, valve status, sensor node health, or emergency protocols.")
                    .intent("GREETING")
                    .suggestions(List.of("System health overview", "Any active leaks?", "Valve positions", "Low battery nodes"))
                    .contextData(Map.of("timestamp", LocalDateTime.now().toString()))
                    .build();
        }

        String lower = msg.toLowerCase();

        // 1. Safety protocols / Action advice
        if (lower.contains("protocol") || lower.contains("what should i do") || lower.contains("procedure") || lower.contains("help") || lower.contains("emergency action")) {
            return handleProtocolQuery();
        }

        // 2. Alerts / Leaks queries
        if (lower.contains("alert") || lower.contains("leak") || lower.contains("warning") || lower.contains("danger")) {
            return handleAlertsQuery();
        }

        // 3. Alarms queries
        if (lower.contains("alarm") || lower.contains("siren") || lower.contains("silence")) {
            return handleAlarmsQuery();
        }

        // 4. Valve queries
        if (lower.contains("valve") || lower.contains("shutoff") || lower.contains("isolation") || lower.contains("open") || lower.contains("close")) {
            return handleValvesQuery();
        }

        // 5. Battery / Node diagnostics
        if (lower.contains("battery") || lower.contains("offline") || lower.contains("node") || lower.contains("sensor")) {
            return handleNodesQuery(lower);
        }

        // 6. Zone status
        if (lower.contains("zone") || lower.contains("pipeline") || lower.contains("status") || lower.contains("health") || lower.contains("overview")) {
            return handleSystemOverviewQuery();
        }

        // Default intelligent fallback with live snapshot
        return handleGeneralQuery(msg);
    }

    private ChatResponse handleAlertsQuery() {
        List<Alert> active = alertRepository.findByStatusNotOrderByCreatedAtDesc("RESOLVED");
        long critical = active.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity())).count();

        String reply;
        if (active.isEmpty()) {
            reply = "Great news: There are currently no active alerts or leaks detected. All pipeline segments are operating within normal parameters.";
        } else {
            String details = active.stream()
                    .limit(3)
                    .map(a -> String.format("• [%s] %s (%s, Node %s)",
                            a.getSeverity(), a.getTitle(),
                            a.getZone() != null ? a.getZone().getName() : "-",
                            a.getNode() != null ? a.getNode().getNodeCode() : "-"))
                    .collect(Collectors.joining("\n"));

            reply = String.format("There are currently %d active alert(s), including %d CRITICAL leak event(s):\n\n%s\n\nImmediate review of critical alerts is strongly advised.",
                    active.size(), critical, details);
        }

        return ChatResponse.builder()
                .reply(reply)
                .intent("ALERT_QUERY")
                .suggestions(List.of("View alarms", "Show valve status", "System safety score"))
                .contextData(Map.of("activeCount", active.size(), "criticalCount", critical))
                .build();
    }

    private ChatResponse handleAlarmsQuery() {
        List<AlarmEvent> openAlarms = alarmRepository.findByStatusInOrderByStartedAtDesc(List.of("ACTIVE", "SILENCED"));

        String reply;
        if (openAlarms.isEmpty()) {
            reply = "No active audio/visual alarms are currently sounding. All sensor readings are below critical alarm thresholds.";
        } else {
            AlarmEvent first = openAlarms.get(0);
            reply = String.format("ATTENTION: %d alarm event(s) are currently open. Most critical: Alarm on Node %s (%s) is %s. Reason: %s.",
                    openAlarms.size(),
                    first.getNode() != null ? first.getNode().getNodeCode() : "-",
                    first.getZone() != null ? first.getZone().getName() : "-",
                    first.getStatus(),
                    first.getMessage());
        }

        return ChatResponse.builder()
                .reply(reply)
                .intent("ALARM_QUERY")
                .suggestions(List.of("Check active leaks", "Valve positions", "System overview"))
                .contextData(Map.of("openAlarmCount", openAlarms.size()))
                .build();
    }

    private ChatResponse handleValvesQuery() {
        List<Zone> zones = zoneRepository.findAll();
        List<SensorNode> nodes = nodeRepository.findAll();

        StringBuilder sb = new StringBuilder("Current Pipeline Valve Status:\n");
        for (Zone z : zones) {
            List<SensorNode> zoneNodes = nodes.stream()
                    .filter(n -> n.getZone() != null && n.getZone().getId().equals(z.getId()))
                    .toList();
            Set<String> states = zoneNodes.stream()
                    .map(n -> n.getValveState() == null ? "OPEN" : n.getValveState())
                    .collect(Collectors.toSet());
            String stateDesc = states.isEmpty() ? "OPEN" : String.join(", ", states);
            sb.append(String.format("• %s: Valve State = %s (%d nodes)\n", z.getName(), stateDesc, zoneNodes.size()));
        }

        Optional<ValveAction> last = valveActionRepository.findTop5ByOrderByPerformedAtDesc().stream().findFirst();
        last.ifPresent(v -> sb.append(String.format("\nLast recorded action: %s in %s by %s (%s).",
                v.getAction(), v.getZone() != null ? v.getZone().getName() : "-", v.getTriggeredBy(), v.getReason())));

        return ChatResponse.builder()
                .reply(sb.toString())
                .intent("VALVE_QUERY")
                .suggestions(List.of("Active alerts", "System health", "Safety protocols"))
                .contextData(Map.of("zoneCount", zones.size()))
                .build();
    }

    private ChatResponse handleNodesQuery(String msg) {
        List<SensorNode> nodes = nodeRepository.findAll();
        List<SensorNode> offline = nodes.stream().filter(n -> "OFFLINE".equalsIgnoreCase(n.getStatus())).toList();
        List<SensorNode> lowBattery = nodes.stream()
                .filter(n -> n.getBatteryLevel() != null && n.getBatteryLevel() <= 20)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Sensor Node Diagnostics (%d Total Nodes):\n", nodes.size()));
        sb.append(String.format("• Online: %d | Offline: %d\n", nodes.size() - offline.size(), offline.size()));

        if (!offline.isEmpty()) {
            sb.append("• OFFLINE Nodes: ")
                    .append(offline.stream().map(SensorNode::getNodeCode).collect(Collectors.joining(", ")))
                    .append(" (Requires immediate check)\n");
        }

        if (!lowBattery.isEmpty()) {
            sb.append("• Low Battery Nodes (≤20%): ")
                    .append(lowBattery.stream().map(n -> n.getNodeCode() + " (" + n.getBatteryLevel() + "%)").collect(Collectors.joining(", ")))
                    .append("\n");
        } else {
            sb.append("• All sensor node batteries are above safe threshold (20%).\n");
        }

        return ChatResponse.builder()
                .reply(sb.toString())
                .intent("NODE_QUERY")
                .suggestions(List.of("Active alerts", "System overview", "Valve positions"))
                .contextData(Map.of("total", nodes.size(), "offline", offline.size(), "lowBattery", lowBattery.size()))
                .build();
    }

    private ChatResponse handleSystemOverviewQuery() {
        List<Zone> zones = zoneRepository.findAll();
        long activeAlerts = alertRepository.countByStatusNot("RESOLVED");
        long criticalAlerts = alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED");
        long totalNodes = nodeRepository.count();
        long onlineNodes = nodeRepository.countByStatus("ONLINE");

        String status = criticalAlerts > 0 ? "CRITICAL HAZARD" : (activeAlerts > 0 ? "ELEVATED RISK" : "OPTIMAL SAFETY");

        String reply = String.format("""
                Pipeline System Overview:
                • Operational Status: %s
                • Monitored Zones: %d
                • Active Sensor Nodes: %d of %d online
                • Active Alerts: %d (Critical: %d)

                Safety Assessment: %s
                """,
                status,
                zones.size(),
                onlineNodes, totalNodes,
                activeAlerts, criticalAlerts,
                criticalAlerts > 0
                        ? "Immediate leak mitigation required. Check active alerts for details."
                        : "All systems operating normally.");

        return ChatResponse.builder()
                .reply(reply)
                .intent("OVERVIEW_QUERY")
                .suggestions(List.of("Any active leaks?", "Valve positions", "Check battery levels"))
                .contextData(Map.of("status", status, "activeAlerts", activeAlerts))
                .build();
    }

    private ChatResponse handleProtocolQuery() {
        String reply = """
                Standard Emergency Response Protocol:
                1. If METHANE CRITICAL is triggered (>2.0% LEL):
                   • Immediately issue emergency valve isolation for the affected zone.
                   • Evacuate personnel within 50 meters of the pipeline junction.
                   • Verify ventilation dampers are open.
                2. If PRESSURE CRITICAL drop occurs (>15 kPa sudden drop):
                   • Inspect upstream regulator and confirm main supply valve position.
                   • Check for catastrophic line rupture or pipe separation.
                3. After hazard containment:
                   • Maintain valve isolation until sensor readings stabilize below warning thresholds for at least 15 minutes.
                   • Resolve alerts in the dashboard before attempting remote valve reopening.
                """;

        return ChatResponse.builder()
                .reply(reply)
                .intent("PROTOCOL_QUERY")
                .suggestions(List.of("System health overview", "Show valve status", "Active alerts"))
                .contextData(Map.of("type", "SAFETY_GUIDE"))
                .build();
    }

    private ChatResponse handleGeneralQuery(String msg) {
        long activeAlerts = alertRepository.countByStatusNot("RESOLVED");
        return ChatResponse.builder()
                .reply(String.format("I processed your query: \"%s\". Currently, the system has %d active alert(s). You can ask me for specific zone details, leak alerts, valve positions, battery levels, or safety protocols.",
                        msg, activeAlerts))
                .intent("GENERAL_QUERY")
                .suggestions(List.of("System health overview", "Any active leaks?", "Valve positions", "Low battery nodes"))
                .contextData(Map.of("activeAlerts", activeAlerts))
                .build();
    }
}
