package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.ZoneStatus;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ValveService {

    private final ZoneRepository zoneRepository;
    private final SensorNodeRepository nodeRepository;
    private final ValveActionRepository valveActionRepository;
    private final AlertRepository alertRepository;
    private final CommandService commandService;

    public List<ZoneStatus> getZoneStatuses() {
        return zoneRepository.findAll().stream().map(zone ->
                ZoneStatus.builder()
                        .id(zone.getId())
                        .name(zone.getName())
                        .riskLevel(zone.getRiskLevel())
                        .valveState(valveStateOf(zone))
                        .build()
        ).toList();
    }

    public ValveAction setValve(Long zoneId, String action, String username) {
        return setValve(zoneId, action, username, true);
    }

    public ValveAction setValve(Long zoneId, String action, String username, boolean isAdmin) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));

        String wanted = action == null ? "" : action.trim().toUpperCase();
        if (!wanted.equals("OPEN") && !wanted.equals("CLOSE")) {
            throw new IllegalArgumentException("Action must be OPEN or CLOSE");
        }

        boolean closing = wanted.equals("CLOSE");

        if (!closing && !isAdmin) {
            throw new SecurityException("Only administrators are permitted to open pipeline valves.");
        }

        if (!closing && (
                alertRepository.existsByZoneIdAndLevelAndSeverityAndStatusNot(zoneId, "SENSOR", "CRITICAL", "RESOLVED")
                || alertRepository.existsByZoneIdAndSeverityAndStatusNot(zoneId, "CRITICAL", "RESOLVED"))) {
            throw new IllegalArgumentException(
                    "Cannot open the valve while a CRITICAL leak alert is open. "
                            + "Fix the cause and resolve the alert first.");
        }

        ValveAction recorded = commandService.issueZoneCommand(
                zone,
                closing ? "CLOSE_VALVE" : "OPEN_VALVE",
                "MANUAL",
                username,
                "Manual " + (closing ? "close" : "open") + " from dashboard");

        if (recorded == null) {
            throw new IllegalArgumentException("The valve in " + zone.getName() + " is already "
                    + (closing ? "closed or closing" : "open or opening"));
        }
        return recorded;
    }

    private String valveStateOf(Zone zone) {
        List<SensorNode> nodes = nodeRepository.findByZoneId(zone.getId());

        if (nodes.isEmpty()) {
            return valveActionRepository.findFirstByZoneIdOrderByPerformedAtDesc(zone.getId())
                    .map(a -> "CLOSE".equals(a.getAction()) ? "CLOSED" : "OPEN")
                    .orElse("OPEN");
        }

        List<SensorNode> onlineNodes = nodes.stream()
                .filter(n -> "ONLINE".equalsIgnoreCase(n.getStatus()))
                .collect(Collectors.toList());
        List<SensorNode> targetNodes = onlineNodes.isEmpty() ? nodes : onlineNodes;

        Set<String> states = targetNodes.stream()
                .map(n -> n.getValveState() == null ? "OPEN" : n.getValveState())
                .collect(Collectors.toSet());

        if (states.contains("FAULT")) return "FAULT";
        if (states.contains("CLOSING")) return "CLOSING";
        if (states.contains("OPENING")) return "OPENING";
        if (states.size() == 1) return states.iterator().next();
        return "PARTIAL";
    }
}