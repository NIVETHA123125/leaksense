package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.ThresholdAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThresholdAuditLogRepository extends JpaRepository<ThresholdAuditLog, Long> {

    List<ThresholdAuditLog> findTop50ByOrderByChangedAtDesc();

    List<ThresholdAuditLog> findByChangedAtBetweenOrderByChangedAtDesc(LocalDateTime from, LocalDateTime to);
}
