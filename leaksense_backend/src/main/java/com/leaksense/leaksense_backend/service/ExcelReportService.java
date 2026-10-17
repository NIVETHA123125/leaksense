package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final AlertRepository alertRepository;

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String time(LocalDateTime value) {
        return value == null ? "" : value.format(FORMAT);
    }

    public byte[] alertsReport() throws IOException {
        List<Alert> alerts = alertRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Alert History");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);

            String[] headers = {"ID", "Title", "Message", "Level", "Severity",
                    "Status", "Zone", "Node", "Acknowledged By", "Created", "Resolved"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            int rowNumber = 1;
            for (Alert a : alerts) {
                Row row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(text(a.getId()));
                row.createCell(1).setCellValue(text(a.getTitle()));
                row.createCell(2).setCellValue(text(a.getMessage()));
                row.createCell(3).setCellValue(text(a.getLevel()));
                row.createCell(4).setCellValue(text(a.getSeverity()));
                row.createCell(5).setCellValue(text(a.getStatus()));
                row.createCell(6).setCellValue(a.getZone() == null ? "" : text(a.getZone().getName()));
                row.createCell(7).setCellValue(a.getNode() == null ? "" : text(a.getNode().getNodeCode()));
                row.createCell(8).setCellValue(text(a.getAcknowledgedBy()));
                row.createCell(9).setCellValue(time(a.getCreatedAt()));
                row.createCell(10).setCellValue(time(a.getResolvedAt()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
