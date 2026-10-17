package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.Reading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {

    Optional<Reading> findFirstByNodeIdOrderByRecordedAtDesc(Long nodeId);

    List<Reading> findTop20ByNodeIdOrderByRecordedAtDesc(Long nodeId);

    List<Reading> findTop50ByOrderByRecordedAtDesc();

    List<Reading> findTop100ByOrderByRecordedAtDesc();

    List<Reading> findTop100ByNodeIdOrderByRecordedAtDesc(Long nodeId);

    List<Reading> findByNodeIdOrderByRecordedAtDesc(Long nodeId, Pageable pageable);

    List<Reading> findByRecordedAtAfterOrderByRecordedAtAsc(LocalDateTime after);

    List<Reading> findByNodeIdAndRecordedAtAfterOrderByRecordedAtAsc(Long nodeId, LocalDateTime after);
}