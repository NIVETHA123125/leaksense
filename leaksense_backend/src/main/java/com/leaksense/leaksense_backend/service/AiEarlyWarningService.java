package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.EarlyWarningResult;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEarlyWarningService {

    private final ReadingRepository readingRepository;

    public Optional<EarlyWarningResult> scanNode(SensorNode node) {
        if (node == null || node.getId() == null) {
            return Optional.empty();
        }

        List<Reading> recent = readingRepository.findByNodeIdOrderByRecordedAtDesc(
                node.getId(), PageRequest.of(0, 10));

        if (recent.size() < 3) {
            return Optional.empty();
        }

        return evaluate(node, recent.get(0), recent);
    }

    public Optional<EarlyWarningResult> evaluate(SensorNode node, Reading latest, List<Reading> history) {
        if (history == null || history.size() < 3) {
            return Optional.empty();
        }

        Reading oldest = history.get(history.size() - 1);
        Reading newest = latest != null ? latest : history.get(0);

        double pressureDelta = newest.getPressureKpa() - oldest.getPressureKpa();
        double methaneDelta = newest.getMethanePpm() - oldest.getMethanePpm();
        double oxygenDelta = newest.getOxygenPercent() - oldest.getOxygenPercent();

        // Pattern 1: Pressure Decay with Gas Creeping Up (Pre-leak signature)
        if (pressureDelta < -1.5 && methaneDelta > 0.2) {
            double confidence = Math.min(0.95, 0.65 + (Math.abs(pressureDelta) * 0.05) + (methaneDelta * 0.1));
            return Optional.of(EarlyWarningResult.builder()
                    .warning(true)
                    .level("HIGH")
                    .confidence(Math.round(confidence * 100.0) / 100.0)
                    .pattern("PRESSURE_DECAY_WITH_GAS_CREEP")
                    .description(String.format("Detected pre-leak correlation: pressure decreased by %.1f kPa while methane increased by %.2f ppm.",
                            Math.abs(pressureDelta), methaneDelta))
                    .recommendation("Dispatch technician to inspect pipeline segment and prepare valve isolation if trend continues.")
                    .build());
        }

        // Pattern 2: Rapid Oxygen Displacement Velocity
        if (oxygenDelta < -0.4 && newest.getOxygenPercent() < 20.2) {
            double confidence = Math.min(0.92, 0.60 + (Math.abs(oxygenDelta) * 0.15));
            return Optional.of(EarlyWarningResult.builder()
                    .warning(true)
                    .level("MEDIUM")
                    .confidence(Math.round(confidence * 100.0) / 100.0)
                    .pattern("OXYGEN_DISPLACEMENT_TRAJECTORY")
                    .description(String.format("Oxygen is falling rapidly (delta: %.2f%%). Trajectory predicts safe threshold breach if displacement continues.",
                            oxygenDelta))
                    .recommendation("Check area ventilation and inspect nearby joints for inert gas or methane displacement.")
                    .build());
        }

        // Pattern 3: High Frequency Micro-Turbulence (Oscillating pressure variance)
        double pressureVariance = calculateVariance(history.stream().mapToDouble(Reading::getPressureKpa).toArray());
        if (pressureVariance > 4.0 && Math.abs(pressureDelta) < 3.0) {
            return Optional.of(EarlyWarningResult.builder()
                    .warning(true)
                    .level("LOW")
                    .confidence(0.72)
                    .pattern("MICRO_LEAK_TURBULENCE")
                    .description(String.format("Pressure fluctuations observed (variance: %.2f) indicating possible valve flutter or pinhole leak turbulence.",
                            pressureVariance))
                    .recommendation("Perform acoustic leak detection or check regulator valve seating during scheduled round.")
                    .build());
        }

        return Optional.empty();
    }

    private double calculateVariance(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.length;
        double sqDiff = 0.0;
        for (double v : values) {
            sqDiff += Math.pow(v - mean, 2);
        }
        return sqDiff / values.length;
    }
}
