package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByStatusNotOrderByCreatedAtDesc(String status);

    long countByStatusNot(String status);

    List<Alert> findByZoneId(Long zoneId);

    Optional<Alert> findFirstByNodeIdAndSensorAndLevelAndStatusNot(
            Long nodeId, String sensor, String level, String status);

    List<Alert> findByNodeIdAndSensorAndLevelAndStatusNot(
            Long nodeId, String sensor, String level, String status);

    boolean existsByZoneIdAndLevelAndSeverityAndStatusNot(
            Long zoneId, String level, String severity, String status);

    boolean existsByZoneIdAndSeverityAndStatusNot(
            Long zoneId, String severity, String status);

    boolean existsByNodeIdAndSensorAndLevelAndStatusNot(
            Long nodeId, String sensor, String level, String status);

    boolean existsByNodeIdAndStatusNot(Long nodeId, String status);

    boolean existsByNodeIdAndLevelAndStatusNot(Long nodeId, String level, String status);

    List<Alert> findByNodeIdAndLevelAndStatusNot(Long nodeId, String level, String status);

    long countBySeverityAndStatusNot(String severity, String status);

    List<Alert> findByCreatedAtAfter(LocalDateTime after);

    List<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
