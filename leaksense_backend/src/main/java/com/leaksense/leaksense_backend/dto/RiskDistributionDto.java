package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskDistributionDto {
    private Map<String, Long> zoneRiskCounts;
    private Map<String, Long> readingRiskCounts;
    private double averageRiskScore;
    private String overallSystemStatus;
}
