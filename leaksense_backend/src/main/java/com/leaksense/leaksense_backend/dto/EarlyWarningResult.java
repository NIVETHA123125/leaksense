package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyWarningResult {
    private boolean warning;
    private String level; // LOW, MEDIUM, HIGH
    private double confidence; // 0.0 - 1.0
    private String pattern; // e.g. PRESSURE_DECAY_WITH_GAS_CREEP
    private String description;
    private String recommendation;
}
