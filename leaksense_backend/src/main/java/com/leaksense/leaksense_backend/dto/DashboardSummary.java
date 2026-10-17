package com.leaksense.leaksense_backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private long totalZones;

    private long totalNodes;

    private long onlineNodes;

    private long offlineNodes;

    private long activeAlerts;

    private long criticalAlerts;
}
