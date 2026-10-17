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
public class ValveActivityStats {
    private long totalActions;
    private long closeCount;
    private long openCount;
    private long manualCount;
    private long autoCount;
    private List<ValveActionSummary> recentActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValveActionSummary {
        private Long id;
        private String action;
        private String zoneName;
        private String triggeredBy;
        private String reason;
        private LocalDateTime performedAt;
    }
}
