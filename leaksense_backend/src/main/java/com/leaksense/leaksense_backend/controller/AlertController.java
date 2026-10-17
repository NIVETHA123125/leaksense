package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/active")
    public List<Alert> getActiveAlerts() {
        return alertService.getActiveAlerts();
    }

    @GetMapping("/history")
    public Page<Alert> getAlertHistory(
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) String sensor,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        return alertService.getAlertHistory(zoneId, nodeId, sensor, level, severity, status, from, to,
                PageRequest.of(Math.max(0, page), Math.max(1, size), sort));
    }

    @PutMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable Long id, @RequestParam String username) {
        return alertService.acknowledge(id, username);
    }

    @PutMapping("/{id}/resolve")
    public Alert resolve(@PathVariable Long id) {
        return alertService.resolve(id);
    }
}
