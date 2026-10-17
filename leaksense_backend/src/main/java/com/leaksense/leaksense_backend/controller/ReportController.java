package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.service.ExcelReportService;
import com.leaksense.leaksense_backend.service.PdfReportService;
import com.leaksense.leaksense_backend.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class ReportController {

    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;
    private final WeeklyReportService weeklyReportService;

    @GetMapping("/alerts/excel")
    public ResponseEntity<byte[]> alertsExcel() throws Exception {
        byte[] data = excelReportService.alertsReport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaksense-alerts.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/alerts/pdf")
    public ResponseEntity<byte[]> alertsPdf() throws Exception {
        byte[] data = pdfReportService.alertsReport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaksense-alerts.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/weekly/pdf")
    public ResponseEntity<byte[]> weeklyPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to)
            throws Exception {
        byte[] data = weeklyReportService.generateWeeklyReport(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leaksense-weekly-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping("/weekly/send-email")
    public ResponseEntity<Map<String, Object>> sendWeeklyEmail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        boolean sent = weeklyReportService.sendWeeklyReportEmail(from, to);
        return ResponseEntity.ok(Map.of(
                "success", sent,
                "message", sent ? "Weekly safety report email dispatched successfully"
                        : "Email dispatch skipped (notifications disabled or delivery failed)"
        ));
    }
}
