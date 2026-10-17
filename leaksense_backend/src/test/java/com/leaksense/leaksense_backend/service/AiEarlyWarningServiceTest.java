package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.EarlyWarningResult;
import com.leaksense.leaksense_backend.entity.Reading;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.ReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEarlyWarningServiceTest {

    @Mock
    private ReadingRepository readingRepository;

    @InjectMocks
    private AiEarlyWarningService service;

    private SensorNode node;
    private Zone zone;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
    }

    @Test
    @DisplayName("Detects pressure decay with gas creeping up (Pre-leak signature)")
    void detectsPressureDecayWithGasCreep() {
        LocalDateTime now = LocalDateTime.now();
        // oldest -> newest: pressure drops by 2.0 kPa, methane rises by 0.5 ppm
        Reading rOld = new Reading(1L, 0.1, 102.0, 20.9, 5.0, now.minusMinutes(5), node);
        Reading rMid = new Reading(2L, 0.3, 101.0, 20.9, 10.0, now.minusMinutes(2), node);
        Reading rNew = new Reading(3L, 0.6, 100.0, 20.9, 18.0, now, node);

        Optional<EarlyWarningResult> result = service.evaluate(node, rNew, List.of(rNew, rMid, rOld));

        assertThat(result).isPresent();
        assertThat(result.get().isWarning()).isTrue();
        assertThat(result.get().getLevel()).isEqualTo("HIGH");
        assertThat(result.get().getPattern()).isEqualTo("PRESSURE_DECAY_WITH_GAS_CREEP");
    }

    @Test
    @DisplayName("Detects rapid oxygen displacement velocity")
    void detectsOxygenDisplacement() {
        LocalDateTime now = LocalDateTime.now();
        Reading rOld = new Reading(1L, 0.1, 101.3, 20.9, 5.0, now.minusMinutes(5), node);
        Reading rMid = new Reading(2L, 0.1, 101.3, 20.5, 10.0, now.minusMinutes(2), node);
        Reading rNew = new Reading(3L, 0.1, 101.3, 20.0, 15.0, now, node);

        Optional<EarlyWarningResult> result = service.evaluate(node, rNew, List.of(rNew, rMid, rOld));

        assertThat(result).isPresent();
        assertThat(result.get().getLevel()).isEqualTo("MEDIUM");
        assertThat(result.get().getPattern()).isEqualTo("OXYGEN_DISPLACEMENT_TRAJECTORY");
    }

    @Test
    @DisplayName("Returns empty when readings are stable and normal")
    void normalReadingsReturnEmpty() {
        LocalDateTime now = LocalDateTime.now();
        Reading rOld = new Reading(1L, 0.1, 101.3, 20.9, 5.0, now.minusMinutes(5), node);
        Reading rMid = new Reading(2L, 0.1, 101.2, 20.9, 5.0, now.minusMinutes(2), node);
        Reading rNew = new Reading(3L, 0.1, 101.3, 20.9, 5.0, now, node);

        Optional<EarlyWarningResult> result = service.evaluate(node, rNew, List.of(rNew, rMid, rOld));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("scanNode calls repository and processes readings")
    void scanNodeQueriesRepo() {
        LocalDateTime now = LocalDateTime.now();
        Reading rOld = new Reading(1L, 0.1, 102.5, 20.9, 5.0, now.minusMinutes(5), node);
        Reading rMid = new Reading(2L, 0.4, 101.0, 20.9, 10.0, now.minusMinutes(2), node);
        Reading rNew = new Reading(3L, 0.7, 100.0, 20.9, 20.0, now, node);

        when(readingRepository.findByNodeIdOrderByRecordedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(rNew, rMid, rOld));

        Optional<EarlyWarningResult> result = service.scanNode(node);

        assertThat(result).isPresent();
        assertThat(result.get().getPattern()).isEqualTo("PRESSURE_DECAY_WITH_GAS_CREEP");
    }
}
