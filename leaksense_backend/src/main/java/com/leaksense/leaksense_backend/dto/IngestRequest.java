package com.leaksense.leaksense_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {

    private String nodeCode;

    private Double methanePpm;

    private Double pressureKpa;

    private Double oxygenPercent;

    private Integer batteryLevel;
}