package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.ZoneStatus;
import com.leaksense.leaksense_backend.service.ValveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zones")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ZoneController {

    private final ValveService valveService;

    @GetMapping
    public List<ZoneStatus> getZones() {
        return valveService.getZoneStatuses();
    }

    @PostMapping("/{id}/valve")
    public ResponseEntity<?> setValve(@PathVariable Long id,
                                      @RequestParam String action,
                                      Authentication authentication) {
        try {
            boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
            String username = authentication != null ? authentication.getName() : "anonymous";
            return ResponseEntity.ok(valveService.setValve(id, action, username, isAdmin));
        } catch (SecurityException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}