package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByName(String name);
}
