package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "threshold_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double methaneWarningPpm;
    private Double methaneLimitPpm;

    private Double pressureNormalKpa;
    private Double pressureToleranceKpa;
    private Double pressureLowWarningKpa;
    private Double pressureLowCriticalKpa;
    private Double pressureHighWarningKpa;
    private Double pressureHighCriticalKpa;

    private Double oxygenLowWarning;
    private Double minSafeOxygen;

    private Double methaneMinValid;
    private Double methaneMaxValid;
    private Double pressureMinValid;
    private Double pressureMaxValid;
    private Double oxygenMinValid;
    private Double oxygenMaxValid;

    private Double warningScore;
    private Double criticalScore;

    private String updatedBy;

    private LocalDateTime updatedAt;
}