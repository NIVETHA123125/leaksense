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
public class SensorTrendPoint {
    private Long id;
    private String nodeCode;
    private double methanePpm;
    private double pressureKpa;
    private double oxygenPercent;
    private double riskScore;
    private LocalDateTime recordedAt;
}
