package com.leaksense.leaksense_backend.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    private PdfReportService pdfReportService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private WeeklyReportService weeklyReportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weeklyReportService, "enabled", true);
        ReflectionTestUtils.setField(weeklyReportService, "recipients", "team@leaksense.com");
        ReflectionTestUtils.setField(weeklyReportService, "from", "report@leaksense.com");
    }

    @Test
    @DisplayName("generateWeeklyReport delegates to pdfReportService")
    void generateWeeklyReportDelegates() throws Exception {
        byte[] expected = "%PDF-test".getBytes();
        when(pdfReportService.generateWeeklyReport(any(), any())).thenReturn(expected);

        byte[] result = weeklyReportService.generateWeeklyReport(null, null);

        assertThat(result).isEqualTo(expected);
        verify(pdfReportService).generateWeeklyReport(any(), any());
    }

    @Test
    @DisplayName("sendWeeklyReportEmail creates and dispatches email when enabled")
    void sendWeeklyReportEmailSuccess() throws Exception {
        byte[] dummyPdf = "%PDF-dummy".getBytes();
        when(pdfReportService.generateWeeklyReport(any(), any())).thenReturn(dummyPdf);

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean sent = weeklyReportService.sendWeeklyReportEmail(
                LocalDateTime.now().minusDays(7), LocalDateTime.now());

        assertThat(sent).isTrue();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendWeeklyReportEmail skips email dispatch when disabled")
    void sendWeeklyReportEmailDisabled() throws Exception {
        ReflectionTestUtils.setField(weeklyReportService, "enabled", false);
        byte[] dummyPdf = "%PDF-dummy".getBytes();
        when(pdfReportService.generateWeeklyReport(any(), any())).thenReturn(dummyPdf);

        boolean sent = weeklyReportService.sendWeeklyReportEmail(null, null);

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendWeeklyReportEmail handles failure gracefully without throwing")
    void sendWeeklyReportEmailHandlesFailure() throws Exception {
        when(pdfReportService.generateWeeklyReport(any(), any()))
                .thenThrow(new RuntimeException("PDF generation crashed"));

        boolean sent = weeklyReportService.sendWeeklyReportEmail(null, null);

        assertThat(sent).isFalse();
    }

    @Test
    @DisplayName("scheduledWeeklyReport triggers without throwing")
    void scheduledWeeklyReportExecutes() throws Exception {
        byte[] dummyPdf = "%PDF-dummy".getBytes();
        when(pdfReportService.generateWeeklyReport(any(), any())).thenReturn(dummyPdf);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatCode(() -> weeklyReportService.scheduledWeeklyReport())
                .doesNotThrowAnyException();
    }
}
