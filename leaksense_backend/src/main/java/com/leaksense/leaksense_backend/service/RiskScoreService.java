package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskScoreService {

    private static final double NORMAL_OXYGEN = 20.9;

    private final SettingsService settingsService;

    public record RiskResult(double score, String level) {}

    public RiskResult calculate(double methanePpm, double pressureKpa, double oxygenPercent) {
        ThresholdSetting s = settingsService.get();

        double methanePart = clamp(methanePpm / s.getMethaneLimitPpm()) * 60;

        double pressurePart = clamp(
                Math.abs(pressureKpa - s.getPressureNormalKpa()) / s.getPressureToleranceKpa()) * 25;

        double oxygenPart = clamp(
                (NORMAL_OXYGEN - oxygenPercent) / (NORMAL_OXYGEN - s.getMinSafeOxygen())) * 15;

        double score = Math.round((methanePart + pressurePart + oxygenPart) * 10.0) / 10.0;
        return new RiskResult(score, levelFor(score, s));
    }

    private String levelFor(double score, ThresholdSetting s) {
        if (score >= s.getCriticalScore()) return "CRITICAL";
        if (score >= s.getWarningScore()) return "WARNING";
        return "SAFE";
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}