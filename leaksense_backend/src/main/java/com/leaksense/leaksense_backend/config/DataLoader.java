package com.leaksense.leaksense_backend.config;

import com.leaksense.leaksense_backend.entity.*;
import com.leaksense.leaksense_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final ZoneRepository zoneRepository;
    private final SensorNodeRepository nodeRepository;
    private final ReadingRepository readingRepository;
    private final AlertRepository alertRepository;
    private final ValveActionRepository valveActionRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner loadSampleData() {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new AppUser(null, "admin",
                        passwordEncoder.encode("Admin@123"), "Nive (Admin)", "ADMIN"));
                userRepository.save(new AppUser(null, "operator",
                        passwordEncoder.encode("Operator@123"), "Site Operator", "OPERATOR"));
            }
            if (zoneRepository.count() > 0) return;

            LocalDateTime now = LocalDateTime.now();

            Zone zoneA = zoneRepository.save(new Zone(null, "Zone A", "Main pipeline section", "SAFE"));
            Zone zoneB = zoneRepository.save(new Zone(null, "Zone B", "Storage area pipeline", "WARNING"));

            SensorNode a1 = nodeRepository.save(new SensorNode(null, "NODE-A1", "ONLINE", 92, now, zoneA));
            SensorNode a2 = nodeRepository.save(new SensorNode(null, "NODE-A2", "ONLINE", 78, now, zoneA));
            SensorNode b1 = nodeRepository.save(new SensorNode(null, "NODE-B1", "OFFLINE", 15, now.minusHours(3), zoneB));

            for (int i = 0; i < 10; i++) {
                readingRepository.save(new Reading(null, 100.0 + i * 20, 450.0 + i, 20.9, 10.0 + i * 3, now.minusMinutes(10 - i), a1));
            }

            alertRepository.save(new Alert(null, "High methane in Zone B", "Methane above safe limit", "ZONE", "CRITICAL", "OPEN", null, now.minusMinutes(20), null, zoneB, b1));
            alertRepository.save(new Alert(null, "Pressure drop in Zone A", "Pressure below normal range", "SENSOR", "MEDIUM", "ACKNOWLEDGED", "Nive", now.minusHours(1), null, zoneA, a2));

            valveActionRepository.save(new ValveAction(null, "CLOSE", "SYSTEM", "Methane above threshold in Zone B", "192.168.1.10", now.minusMinutes(19), zoneB));
        };
    }
}
