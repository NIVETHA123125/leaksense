package com.leaksense.leaksense_backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final PdfReportService pdfReportService;
    private final JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Value("${leaksense.notify.enabled:false}")
    private boolean enabled;

    @Value("${leaksense.notify.to:}")
    private String recipients;

    @Value("${spring.mail.username:}")
    private String from;

    public byte[] generateWeeklyReport(LocalDateTime fromDate, LocalDateTime toDate) throws Exception {
        return pdfReportService.generateWeeklyReport(fromDate, toDate);
    }

    public boolean sendWeeklyReportEmail(LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime start = fromDate != null ? fromDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = toDate != null ? toDate : LocalDateTime.now();

        try {
            byte[] pdfBytes = generateWeeklyReport(start, end);

            if (!enabled || recipients == null || recipients.isBlank() || from == null || from.isBlank()) {
                log.info("Weekly report generated successfully ({} bytes), but email dispatch is not configured or disabled.",
                        pdfBytes.length);
                return false;
            }

            String startStr = start.format(DATE_FORMAT);
            String endStr = end.format(DATE_FORMAT);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(recipients.trim().split("\\s*,\\s*"));
            helper.setSubject(String.format("[LeakSense] Weekly Safety & Incident Report (%s to %s)", startStr, endStr));

            String body = String.format("""
                    LeakSense Weekly Safety & Incident Report
                    Reporting Period: %s to %s

                    The comprehensive weekly pipeline safety report has been generated and is attached to this email.
                    Please review all critical alarm events, valve isolations, and leak warnings in the attached PDF.

                    Access the LeakSense Dashboard: http://localhost:5173
                    """, startStr, endStr);

            helper.setText(body);
            helper.addAttachment(String.format("LeakSense-Weekly-Report-%s.pdf", endStr),
                    new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Weekly report PDF successfully sent via email to {}", recipients);
            return true;
        } catch (Exception e) {
            log.warn("Failed to generate or dispatch weekly report email: {}", e.getMessage());
            return false;
        }
    }

    @Scheduled(cron = "${leaksense.report.weekly-cron:0 0 9 * * MON}")
    public void scheduledWeeklyReport() {
        log.info("Triggering scheduled weekly report generation...");
        LocalDateTime now = LocalDateTime.now();
        sendWeeklyReportEmail(now.minusDays(7), now);
    }
}
