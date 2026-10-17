package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.*;
import com.leaksense.leaksense_backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sensor-trends")
    public List<SensorTrendPoint> getSensorTrends(
            @RequestParam(required = false) Long nodeId,
            @RequestParam(defaultValue = "24") int hours) {
        return analyticsService.getSensorTrends(nodeId, hours);
    }

    @GetMapping("/risk-distribution")
    public RiskDistributionDto getRiskDistribution() {
        return analyticsService.getRiskDistribution();
    }

    @GetMapping("/alerts-timeline")
    public List<AlertTimelinePoint> getAlertTimeline(
            @RequestParam(defaultValue = "7") int days) {
        return analyticsService.getAlertTimeline(days);
    }

    @GetMapping("/alerts-by-sensor")
    public List<AlertCategoryDistribution> getAlertsBySensor() {
        return analyticsService.getAlertsBySensor();
    }

    @GetMapping("/valve-activity")
    public ValveActivityStats getValveActivityStats() {
        return analyticsService.getValveActivityStats();
    }
}
