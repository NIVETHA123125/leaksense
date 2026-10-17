package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.service.ExcelReportService;
import com.leaksense.leaksense_backend.service.PdfReportService;
import com.leaksense.leaksense_backend.service.WeeklyReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ExcelReportService excelReportService;

    @Mock
    private PdfReportService pdfReportService;

    @Mock
    private WeeklyReportService weeklyReportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    @DisplayName("alertsExcel returns excel attachment")
    void alertsExcel() throws Exception {
        when(excelReportService.alertsReport()).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = reportController.alertsExcel();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
        verify(excelReportService).alertsReport();
    }

    @Test
    @DisplayName("alertsPdf returns pdf attachment")
    void alertsPdf() throws Exception {
        when(pdfReportService.alertsReport()).thenReturn(new byte[]{4, 5, 6});

        ResponseEntity<byte[]> response = reportController.alertsPdf();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new byte[]{4, 5, 6});
        verify(pdfReportService).alertsReport();
    }

    @Test
    @DisplayName("weeklyPdf returns weekly report pdf attachment")
    void weeklyPdf() throws Exception {
        when(weeklyReportService.generateWeeklyReport(any(), any())).thenReturn(new byte[]{7, 8, 9});

        ResponseEntity<byte[]> response = reportController.weeklyPdf(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(new byte[]{7, 8, 9});
        verify(weeklyReportService).generateWeeklyReport(any(), any());
    }

    @Test
    @DisplayName("sendWeeklyEmail returns response map with success status")
    void sendWeeklyEmail() {
        when(weeklyReportService.sendWeeklyReportEmail(any(), any())).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = reportController.sendWeeklyEmail(null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        verify(weeklyReportService).sendWeeklyReportEmail(any(), any());
    }
}
