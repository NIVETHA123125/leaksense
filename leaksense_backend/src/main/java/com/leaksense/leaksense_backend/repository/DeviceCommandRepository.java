package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    List<DeviceCommand> findByNodeIdAndStatusIn(Long nodeId, Collection<String> statuses);

    List<DeviceCommand> findByStatusIn(Collection<String> statuses);
}
