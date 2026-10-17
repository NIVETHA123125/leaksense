package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.ThresholdRequest;
import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import com.leaksense.leaksense_backend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/thresholds")
    public ThresholdSetting getThresholds() {
        return settingsService.get();
    }

    @GetMapping("/history")
    public java.util.List<com.leaksense.leaksense_backend.entity.ThresholdAuditLog> getThresholdHistory() {
        return settingsService.getAuditHistory();
    }

    @PutMapping("/thresholds")
    public ResponseEntity<?> updateThresholds(@RequestBody ThresholdRequest request,
                                              Authentication authentication) {
        try {
            return ResponseEntity.ok(settingsService.update(request, authentication.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
