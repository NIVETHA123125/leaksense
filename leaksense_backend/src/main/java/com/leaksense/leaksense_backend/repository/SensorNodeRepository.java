package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.SensorNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SensorNodeRepository extends JpaRepository<SensorNode, Long> {

    Optional<SensorNode> findByNodeCode(String nodeCode);

    List<SensorNode> findByZoneId(Long zoneId);

    long countByStatus(String status);
}
