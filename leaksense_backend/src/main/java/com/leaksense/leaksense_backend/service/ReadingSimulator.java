package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
@ConditionalOnProperty(name = "leaksense.simulator.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReadingSimulator {

    private final SensorNodeRepository nodeRepository;
    private final ReadingService readingService;
    private final CommandService commandService;
    private final Random random = new Random();

    @Scheduled(fixedRate = 5000)
    public void generateReading() {
        SensorNode node = nodeRepository.findByNodeCode("NODE-A1").orElse(null);
        if (node == null) return;

        double methane = 100 + random.nextInt(300);
        double pressure = 465 - methane / 20.0 + random.nextInt(5);
        double oxygen = Math.round((20.9 - methane / 1000.0 - random.nextDouble() * 0.2) * 10.0) / 10.0;

        readingService.process(node, methane, pressure, oxygen, null);
        commandService.simulateDeviceReplies(node);
    }
}