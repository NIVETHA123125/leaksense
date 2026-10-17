package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsight {
    private String id;
    private String zoneName;
    private String nodeCode;
    private String severity; // INFO, WARNING, CRITICAL
    private String type; // EARLY_WARNING, ANOMALY, PREDICTION, MAINTENANCE
    private String title;
    private String description;
    private double confidenceScore;
    private String recommendation;
    private LocalDateTime detectedAt;
}
