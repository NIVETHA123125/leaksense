package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ThresholdEvaluator {

    public enum Status { NORMAL, WARNING, CRITICAL, FAULT }

    public record SensorCheck(String sensor, String label, String unit, Status status,
                              String direction, double value, double limit) {}

    public record Evaluation(SensorCheck methane, SensorCheck pressure, SensorCheck oxygen) {
        public List<SensorCheck> all() {
            return List.of(methane, pressure, oxygen);
        }
    }

    public Evaluation evaluate(double methane, double pressure, double oxygen, ThresholdSetting s) {
        return new Evaluation(
                checkMethane(methane, s),
                checkPressure(pressure, s),
                checkOxygen(oxygen, s));
    }

    private SensorCheck checkMethane(double v, ThresholdSetting s) {
        if (v < s.getMethaneMinValid()) return methane(Status.FAULT, "LOW", v, s.getMethaneMinValid());
        if (v > s.getMethaneMaxValid()) return methane(Status.FAULT, "HIGH", v, s.getMethaneMaxValid());
        if (v > s.getMethaneLimitPpm()) return methane(Status.CRITICAL, "HIGH", v, s.getMethaneLimitPpm());
        if (v > s.getMethaneWarningPpm()) return methane(Status.WARNING, "HIGH", v, s.getMethaneWarningPpm());
        return methane(Status.NORMAL, null, v, s.getMethaneWarningPpm());
    }

    private SensorCheck checkPressure(double v, ThresholdSetting s) {
        if (v < s.getPressureMinValid()) return pressure(Status.FAULT, "LOW", v, s.getPressureMinValid());
        if (v > s.getPressureMaxValid()) return pressure(Status.FAULT, "HIGH", v, s.getPressureMaxValid());
        if (v < s.getPressureLowCriticalKpa()) return pressure(Status.CRITICAL, "LOW", v, s.getPressureLowCriticalKpa());
        if (v > s.getPressureHighCriticalKpa()) return pressure(Status.CRITICAL, "HIGH", v, s.getPressureHighCriticalKpa());
        if (v < s.getPressureLowWarningKpa()) return pressure(Status.WARNING, "LOW", v, s.getPressureLowWarningKpa());
        if (v > s.getPressureHighWarningKpa()) return pressure(Status.WARNING, "HIGH", v, s.getPressureHighWarningKpa());
        return pressure(Status.NORMAL, null, v, s.getPressureNormalKpa());
    }

    private SensorCheck checkOxygen(double v, ThresholdSetting s) {
        if (v < s.getOxygenMinValid()) return oxygen(Status.FAULT, "LOW", v, s.getOxygenMinValid());
        if (v > s.getOxygenMaxValid()) return oxygen(Status.FAULT, "HIGH", v, s.getOxygenMaxValid());
        if (v < s.getMinSafeOxygen()) return oxygen(Status.CRITICAL, "LOW", v, s.getMinSafeOxygen());
        if (v < s.getOxygenLowWarning()) return oxygen(Status.WARNING, "LOW", v, s.getOxygenLowWarning());
        return oxygen(Status.NORMAL, null, v, s.getOxygenLowWarning());
    }

    private SensorCheck methane(Status status, String direction, double value, double limit) {
        return new SensorCheck("METHANE", "Methane", "ppm", status, direction, value, limit);
    }

    private SensorCheck pressure(Status status, String direction, double value, double limit) {
        return new SensorCheck("PRESSURE", "Pressure", "kPa", status, direction, value, limit);
    }

    private SensorCheck oxygen(Status status, String direction, double value, double limit) {
        return new SensorCheck("OXYGEN", "Oxygen", "%", status, direction, value, limit);
    }
}
