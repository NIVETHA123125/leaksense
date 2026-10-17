package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/active")
    public List<AlarmEvent> active() {
        return alarmService.active();
    }

    @GetMapping("/recent")
    public List<AlarmEvent> recent() {
        return alarmService.recent();
    }

    @PutMapping("/{id}/silence")
    public ResponseEntity<?> silence(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(alarmService.silence(id, authentication.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/silence-all")
    public ResponseEntity<?> silenceAll(Authentication authentication) {
        int count = alarmService.silenceAll(authentication.getName());
        return ResponseEntity.ok(Map.of("silenced", count));
    }
}
