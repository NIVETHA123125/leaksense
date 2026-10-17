package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTimelinePoint {
    private String date;
    private long criticalCount;
    private long warningCount;
    private long infoCount;
    private long totalCount;
}
