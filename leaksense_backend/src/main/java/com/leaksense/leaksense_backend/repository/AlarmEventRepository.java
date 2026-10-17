package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {

    List<AlarmEvent> findByStatusInOrderByStartedAtDesc(Collection<String> statuses);

    Optional<AlarmEvent> findFirstByNodeIdAndStatusIn(Long nodeId, Collection<String> statuses);

    List<AlarmEvent> findTop20ByOrderByStartedAtDesc();

    List<AlarmEvent> findByStartedAtBetweenOrderByStartedAtDesc(LocalDateTime from, LocalDateTime to);
}