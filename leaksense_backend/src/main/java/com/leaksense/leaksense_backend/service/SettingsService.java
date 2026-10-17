package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.ThresholdRequest;
import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import com.leaksense.leaksense_backend.repository.ThresholdSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final ThresholdSettingRepository repository;
    private final com.leaksense.leaksense_backend.repository.ThresholdAuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public ThresholdSetting get() {
        ThresholdSetting setting = repository.findAll().stream().findFirst()
                .orElseGet(() -> repository.save(defaults()));

        if (fillMissingDefaults(setting)) {
            return repository.save(setting);
        }
        return setting;
    }

    public com.leaksense.leaksense_backend.entity.ThresholdAuditLog saveAudit(com.leaksense.leaksense_backend.entity.ThresholdAuditLog log) {
        return auditLogRepository != null ? auditLogRepository.save(log) : null;
    }

    public List<com.leaksense.leaksense_backend.entity.ThresholdAuditLog> getAuditHistory() {
        return auditLogRepository != null ? auditLogRepository.findTop50ByOrderByChangedAtDesc() : List.of();
    }

    public ThresholdSetting update(ThresholdRequest r, String username) {
        validate(r);

        ThresholdSetting s = get();
        LocalDateTime now = LocalDateTime.now();

        auditIfChanged("methaneWarningPpm", s.getMethaneWarningPpm(), r.getMethaneWarningPpm(), username, now);
        auditIfChanged("methaneLimitPpm", s.getMethaneLimitPpm(), r.getMethaneLimitPpm(), username, now);
        auditIfChanged("pressureNormalKpa", s.getPressureNormalKpa(), r.getPressureNormalKpa(), username, now);
        auditIfChanged("pressureToleranceKpa", s.getPressureToleranceKpa(), r.getPressureToleranceKpa(), username, now);
        auditIfChanged("pressureLowWarningKpa", s.getPressureLowWarningKpa(), r.getPressureLowWarningKpa(), username, now);
        auditIfChanged("pressureLowCriticalKpa", s.getPressureLowCriticalKpa(), r.getPressureLowCriticalKpa(), username, now);
        auditIfChanged("pressureHighWarningKpa", s.getPressureHighWarningKpa(), r.getPressureHighWarningKpa(), username, now);
        auditIfChanged("pressureHighCriticalKpa", s.getPressureHighCriticalKpa(), r.getPressureHighCriticalKpa(), username, now);
        auditIfChanged("oxygenLowWarning", s.getOxygenLowWarning(), r.getOxygenLowWarning(), username, now);
        auditIfChanged("minSafeOxygen", s.getMinSafeOxygen(), r.getMinSafeOxygen(), username, now);
        auditIfChanged("warningScore", s.getWarningScore(), r.getWarningScore(), username, now);
        auditIfChanged("criticalScore", s.getCriticalScore(), r.getCriticalScore(), username, now);

        s.setMethaneWarningPpm(r.getMethaneWarningPpm());
        s.setMethaneLimitPpm(r.getMethaneLimitPpm());
        s.setPressureNormalKpa(r.getPressureNormalKpa());
        s.setPressureToleranceKpa(r.getPressureToleranceKpa());
        s.setPressureLowWarningKpa(r.getPressureLowWarningKpa());
        s.setPressureLowCriticalKpa(r.getPressureLowCriticalKpa());
        s.setPressureHighWarningKpa(r.getPressureHighWarningKpa());
        s.setPressureHighCriticalKpa(r.getPressureHighCriticalKpa());
        s.setOxygenLowWarning(r.getOxygenLowWarning());
        s.setMinSafeOxygen(r.getMinSafeOxygen());
        s.setMethaneMinValid(r.getMethaneMinValid());
        s.setMethaneMaxValid(r.getMethaneMaxValid());
        s.setPressureMinValid(r.getPressureMinValid());
        s.setPressureMaxValid(r.getPressureMaxValid());
        s.setOxygenMinValid(r.getOxygenMinValid());
        s.setOxygenMaxValid(r.getOxygenMaxValid());
        s.setWarningScore(r.getWarningScore());
        s.setCriticalScore(r.getCriticalScore());
        s.setUpdatedBy(username);
        s.setUpdatedAt(now);
        ThresholdSetting saved = repository.save(s);
        if (notificationService != null) {
            notificationService.sendThresholdChanged(username, "Methane: " + s.getMethaneLimitPpm() + " ppm, Press: " + s.getPressureLowCriticalKpa() + " kPa");
        }
        return saved;
    }

    private void auditIfChanged(String param, Double oldVal, Double newVal, String username, LocalDateTime now) {
        if (auditLogRepository != null && oldVal != null && newVal != null && Double.compare(oldVal, newVal) != 0) {
            auditLogRepository.save(new com.leaksense.leaksense_backend.entity.ThresholdAuditLog(
                    null, param, oldVal, newVal, username, now));
        }
    }

    private ThresholdSetting defaults() {
        ThresholdSetting s = new ThresholdSetting();
        s.setMethaneLimitPpm(350.0);
        s.setPressureNormalKpa(460.0);
        s.setPressureToleranceKpa(40.0);
        s.setMinSafeOxygen(19.5);
        s.setWarningScore(30.0);
        s.setCriticalScore(60.0);
        fillMissingDefaults(s);
        s.setUpdatedBy("SYSTEM");
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private boolean fillMissingDefaults(ThresholdSetting s) {
        boolean changed = false;
        changed |= fill(s.getMethaneWarningPpm(), s::setMethaneWarningPpm, 200.0);
        changed |= fill(s.getPressureLowWarningKpa(), s::setPressureLowWarningKpa, 440.0);
        changed |= fill(s.getPressureLowCriticalKpa(), s::setPressureLowCriticalKpa, 420.0);
        changed |= fill(s.getPressureHighWarningKpa(), s::setPressureHighWarningKpa, 480.0);
        changed |= fill(s.getPressureHighCriticalKpa(), s::setPressureHighCriticalKpa, 500.0);
        changed |= fill(s.getOxygenLowWarning(), s::setOxygenLowWarning, 20.0);
        changed |= fill(s.getMethaneMinValid(), s::setMethaneMinValid, 0.0);
        changed |= fill(s.getMethaneMaxValid(), s::setMethaneMaxValid, 10000.0);
        changed |= fill(s.getPressureMinValid(), s::setPressureMinValid, 15.0);
        changed |= fill(s.getPressureMaxValid(), s::setPressureMaxValid, 700.0);
        changed |= fill(s.getOxygenMinValid(), s::setOxygenMinValid, 0.0);
        changed |= fill(s.getOxygenMaxValid(), s::setOxygenMaxValid, 25.0);
        return changed;
    }

    private boolean fill(Double current, Consumer<Double> setter, double fallback) {
        if (current != null) {
            return false;
        }
        setter.accept(fallback);
        return true;
    }

    private boolean anyNull(Double... values) {
        for (Double v : values) {
            if (v == null) {
                return true;
            }
        }
        return false;
    }

    private void validate(ThresholdRequest r) {
        if (anyNull(r.getMethaneWarningPpm(), r.getMethaneLimitPpm(),
                r.getPressureNormalKpa(), r.getPressureToleranceKpa(),
                r.getPressureLowWarningKpa(), r.getPressureLowCriticalKpa(),
                r.getPressureHighWarningKpa(), r.getPressureHighCriticalKpa(),
                r.getOxygenLowWarning(), r.getMinSafeOxygen(),
                r.getMethaneMinValid(), r.getMethaneMaxValid(),
                r.getPressureMinValid(), r.getPressureMaxValid(),
                r.getOxygenMinValid(), r.getOxygenMaxValid(),
                r.getWarningScore(), r.getCriticalScore())) {
            throw new IllegalArgumentException("All threshold values are required");
        }
        if (r.getMethaneLimitPpm() < 1 || r.getMethaneLimitPpm() > 5000) {
            throw new IllegalArgumentException("Methane limit must be between 1 and 5000 ppm");
        }
        if (r.getPressureNormalKpa() <= 0) {
            throw new IllegalArgumentException("Normal pressure must be above 0 kPa");
        }
        if (r.getPressureToleranceKpa() <= 0) {
            throw new IllegalArgumentException("Pressure tolerance must be above 0 kPa");
        }
        if (r.getMinSafeOxygen() < 10 || r.getMinSafeOxygen() > 20.8) {
            throw new IllegalArgumentException("Minimum safe oxygen must be between 10 and 20.8 percent");
        }
        if (r.getWarningScore() <= 0 || r.getCriticalScore() > 100
                || r.getWarningScore() >= r.getCriticalScore()) {
            throw new IllegalArgumentException(
                    "Scores must satisfy 0 < warning < critical <= 100");
        }

        if (r.getMethaneWarningPpm() <= 0 || r.getMethaneWarningPpm() >= r.getMethaneLimitPpm()) {
            throw new IllegalArgumentException(
                    "Methane warning must be above 0 and below the critical methane limit");
        }
        if (!(r.getPressureLowCriticalKpa() < r.getPressureLowWarningKpa()
                && r.getPressureLowWarningKpa() < r.getPressureNormalKpa()
                && r.getPressureNormalKpa() < r.getPressureHighWarningKpa()
                && r.getPressureHighWarningKpa() < r.getPressureHighCriticalKpa())) {
            throw new IllegalArgumentException(
                    "Pressure limits must satisfy: low critical < low warning < normal < high warning < high critical");
        }
        if (r.getOxygenLowWarning() <= r.getMinSafeOxygen() || r.getOxygenLowWarning() > 20.8) {
            throw new IllegalArgumentException(
                    "Oxygen warning must be above the critical oxygen level and at most 20.8 percent");
        }
        if (r.getMethaneMinValid() >= r.getMethaneMaxValid()
                || r.getMethaneMaxValid() <= r.getMethaneLimitPpm()) {
            throw new IllegalArgumentException(
                    "Methane sensor range must be valid and above the critical limit");
        }
        if (r.getPressureMinValid() >= r.getPressureLowCriticalKpa()
                || r.getPressureMaxValid() <= r.getPressureHighCriticalKpa()) {
            throw new IllegalArgumentException(
                    "Pressure sensor range must contain all the pressure limits");
        }
        if (r.getOxygenMinValid() >= r.getMinSafeOxygen() || r.getOxygenMaxValid() <= 20.9) {
            throw new IllegalArgumentException(
                    "Oxygen sensor range must contain the oxygen limits and normal air (20.9 percent)");
        }
    }
}