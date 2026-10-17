package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportSummary {
    private String executiveSummary;
    private String riskLevel;
    private double overallSafetyScore; // 0 - 100
    private List<String> keyObservations;
    private List<String> recommendedActions;
    private LocalDateTime generatedAt;
}
