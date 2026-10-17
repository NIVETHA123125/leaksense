package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.ValveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValveActionRepository extends JpaRepository<ValveAction, Long> {

    List<ValveAction> findTop5ByOrderByPerformedAtDesc();

    List<ValveAction> findTop10ByOrderByPerformedAtDesc();

    List<ValveAction> findByZoneIdOrderByPerformedAtDesc(Long zoneId);

    Optional<ValveAction> findFirstByZoneIdOrderByPerformedAtDesc(Long zoneId);

    List<ValveAction> findByPerformedAtAfter(LocalDateTime after);

    long countByAction(String action);

    List<ValveAction> findByPerformedAtBetweenOrderByPerformedAtDesc(LocalDateTime from, LocalDateTime to);
}
