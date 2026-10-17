package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class SensorNodeController {

    private final SensorNodeRepository sensorNodeRepository;

    @GetMapping
    public List<SensorNode> getAllNodes() {
        return sensorNodeRepository.findAll();
    }

    @GetMapping("/zone/{zoneId}")
    public List<SensorNode> getNodesByZone(@PathVariable Long zoneId) {
        return sensorNodeRepository.findByZoneId(zoneId);
    }
}
