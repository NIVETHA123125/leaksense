package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.repository.ValveActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/valve-actions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ValveActionController {

    private final ValveActionRepository valveActionRepository;

    @GetMapping("/recent")
    public List<ValveAction> getRecentActions() {
        return valveActionRepository.findTop5ByOrderByPerformedAtDesc();
    }

    @GetMapping("/zone/{zoneId}")
    public List<ValveAction> getActionsByZone(@PathVariable Long zoneId) {
        return valveActionRepository.findByZoneIdOrderByPerformedAtDesc(zoneId);
    }
}