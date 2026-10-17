package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.DashboardSummary;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ZoneRepository zoneRepository;
    private final SensorNodeRepository sensorNodeRepository;
    private final AlertRepository alertRepository;

    public DashboardSummary getSummary() {
        long totalNodes = sensorNodeRepository.count();
        long onlineNodes = sensorNodeRepository.countByStatus("ONLINE");

        return DashboardSummary.builder()
                .totalZones(zoneRepository.count())
                .totalNodes(totalNodes)
                .onlineNodes(onlineNodes)
                .offlineNodes(totalNodes - onlineNodes)
                .activeAlerts(alertRepository.countByStatusNot("RESOLVED"))
                .criticalAlerts(alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED"))
                .build();
    }
}