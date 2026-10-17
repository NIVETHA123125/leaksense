package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.CommandAckRequest;
import com.leaksense.leaksense_backend.dto.IngestRequest;
import com.leaksense.leaksense_backend.entity.DeviceCommand;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.service.CommandService;
import com.leaksense.leaksense_backend.service.ReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final SensorNodeRepository nodeRepository;
    private final ReadingService readingService;
    private final CommandService commandService;

    @Value("${leaksense.device.api-key}")
    private String deviceApiKey;

    private boolean authorized(String apiKey) {
        return apiKey != null && MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                deviceApiKey.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("message", "Invalid device key"));
    }

    private List<Map<String, Object>> toPayload(List<DeviceCommand> commands) {
        return commands.stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "type", c.getType()))
                .toList();
    }

    @PostMapping("/readings")
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody IngestRequest request) {

        if (!authorized(apiKey)) {
            return unauthorized();
        }

        if (request.getNodeCode() == null || request.getMethanePpm() == null
                || request.getPressureKpa() == null || request.getOxygenPercent() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "nodeCode, methanePpm, pressureKpa and oxygenPercent are required"));
        }

        SensorNode node = nodeRepository.findByNodeCode(request.getNodeCode()).orElse(null);
        if (node == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "Unknown node: " + request.getNodeCode()));
        }

        Reading saved = readingService.process(
                node,
                request.getMethanePpm(),
                request.getPressureKpa(),
                request.getOxygenPercent(),
                request.getBatteryLevel());

        List<DeviceCommand> commands = commandService.deliverPending(node);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "riskScore", saved.getRiskScore(),
                "commands", toPayload(commands)));
    }

    @GetMapping("/commands")
    public ResponseEntity<?> commands(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam String nodeCode) {

        if (!authorized(apiKey)) {
            return unauthorized();
        }

        SensorNode node = nodeRepository.findByNodeCode(nodeCode).orElse(null);
        if (node == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Unknown node: " + nodeCode));
        }

        return ResponseEntity.ok(Map.of("commands", toPayload(commandService.deliverPending(node))));
    }

    @PostMapping("/commands/{id}/ack")
    public ResponseEntity<?> acknowledge(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable Long id,
            @RequestBody CommandAckRequest request) {

        if (!authorized(apiKey)) {
            return unauthorized();
        }

        if (request.getNodeCode() == null || request.getSuccess() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "nodeCode and success are required"));
        }

        try {
            DeviceCommand command = commandService.acknowledge(
                    id, request.getNodeCode(), request.getSuccess(), request.getNote());
            return ResponseEntity.ok(Map.of("status", command.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}