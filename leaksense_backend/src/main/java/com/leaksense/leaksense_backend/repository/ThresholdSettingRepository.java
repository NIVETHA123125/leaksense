package com.leaksense.leaksense_backend.repository;

import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdSettingRepository extends JpaRepository<ThresholdSetting, Long> {
}
