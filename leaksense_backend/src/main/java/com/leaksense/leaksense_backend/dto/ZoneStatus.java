package com.leaksense.leaksense_backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneStatus {

    private Long id;

    private String name;

    private String riskLevel;

    private String valveState;
}