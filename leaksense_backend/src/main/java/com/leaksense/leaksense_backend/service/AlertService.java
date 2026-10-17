package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusNotOrderByCreatedAtDesc("RESOLVED");
    }

    public Page<Alert> getAlertHistory(Long zoneId, Long nodeId, String sensor,
                                       String level, String severity, String status,
                                       LocalDateTime from, LocalDateTime to,
                                       Pageable pageable) {
        Specification<Alert> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zone").get("id"), zoneId));
            }
            if (nodeId != null) {
                predicates.add(cb.equal(root.get("node").get("id"), nodeId));
            }
            if (sensor != null && !sensor.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("sensor")), sensor.trim().toLowerCase()));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("level")), level.trim().toUpperCase()));
            }
            if (severity != null && !severity.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("severity")), severity.trim().toUpperCase()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return alertRepository.findAll(spec, pageable);
    }

    public Alert acknowledge(Long id, String username) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(username);
        return alertRepository.save(alert);
    }

    public Alert resolve(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }
}
