package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.*;
import com.leaksense.leaksense_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlarmEventRepository alarmRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private SensorNodeRepository nodeRepository;

    @InjectMocks
    private PdfReportService pdfReportService;

    private Zone zone;
    private SensorNode node;
    private Alert alert;
    private AlarmEvent alarm;
    private ValveAction valveAction;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
        alert = new Alert(1L, "Methane Critical", "Explosion risk", "SENSOR", "CRITICAL",
                "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");
        alarm = new AlarmEvent(1L, node, zone, "methane", "Methane at 3.2%", "ACTIVE",
                LocalDateTime.now(), null, null, null);
        valveAction = new ValveAction(1L, "CLOSE", "SYSTEM", "Automatic isolation", "127.0.0.1",
                LocalDateTime.now(), zone);
    }

    @Test
    @DisplayName("alertsReport generates valid non-empty PDF bytes")
    void alertsReportGeneratesPdf() throws Exception {
        when(alertRepository.findAll(any(Sort.class))).thenReturn(List.of(alert));

        byte[] pdf = pdfReportService.alertsReport();

        assertThat(pdf).isNotNull().isNotEmpty();
        // PDF magic bytes %PDF
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("generateWeeklyReport with data generates valid non-empty PDF bytes")
    void generateWeeklyReportWithData() throws Exception {
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(alert));
        when(alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(any(), any()))
                .thenReturn(List.of(alarm));
        when(valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(any(), any()))
                .thenReturn(List.of(valveAction));
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(nodeRepository.findAll()).thenReturn(List.of(node));

        byte[] pdf = pdfReportService.generateWeeklyReport(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("generateWeeklyReport with empty tables generates valid PDF safely")
    void generateWeeklyReportEmptyData() throws Exception {
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(zoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(nodeRepository.findAll()).thenReturn(Collections.emptyList());

        byte[] pdf = pdfReportService.generateWeeklyReport(null, null);

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
