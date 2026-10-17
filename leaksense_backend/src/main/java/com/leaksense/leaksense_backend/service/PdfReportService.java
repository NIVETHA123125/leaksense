package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final AlertRepository alertRepository;
    private final AlarmEventRepository alarmRepository;
    private final ValveActionRepository valveActionRepository;
    private final ZoneRepository zoneRepository;
    private final SensorNodeRepository nodeRepository;

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private static final Color BRAND_DARK = new Color(15, 23, 42); // slate-900
    private static final Color BRAND_PRIMARY = new Color(13, 148, 136); // teal-600
    private static final Color BRAND_LIGHT = new Color(240, 253, 250); // teal-50
    private static final Color ROW_ALT = new Color(248, 250, 252); // slate-50
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // slate-200

    private String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String time(LocalDateTime value) {
        return value == null ? "-" : value.format(FORMAT);
    }

    public byte[] alertsReport() throws Exception {
        List<Alert> alerts = alertRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND_DARK);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        document.add(new Paragraph("LeakSense Alert Report", titleFont));
        document.add(new Paragraph("Generated on " + time(LocalDateTime.now())
                + "  |  Total alerts: " + alerts.size(), subFont));
        document.add(new Paragraph(" "));

        String[] headers = {"ID", "Title", "Severity", "Status", "Zone", "Node",
                "Acknowledged By", "Created"};

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 4f, 2f, 2.5f, 2f, 2f, 2.5f, 3f});

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(BRAND_PRIMARY);
            cell.setPadding(6);
            table.addCell(cell);
        }

        for (Alert a : alerts) {
            String[] values = {
                    text(a.getId()),
                    text(a.getTitle()),
                    text(a.getSeverity()),
                    text(a.getStatus()),
                    a.getZone() == null ? "" : text(a.getZone().getName()),
                    a.getNode() == null ? "" : text(a.getNode().getNodeCode()),
                    text(a.getAcknowledgedBy()),
                    time(a.getCreatedAt())
            };
            for (String v : values) {
                PdfPCell cell = new PdfPCell(new Phrase(v, bodyFont));
                cell.setPadding(5);
                table.addCell(cell);
            }
        }

        table.setHeaderRows(1);
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public byte[] generateWeeklyReport(LocalDateTime from, LocalDateTime to) throws Exception {
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(7);
        LocalDateTime end = to != null ? to : LocalDateTime.now();

        List<Alert> alerts = alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        List<AlarmEvent> alarms = alarmRepository.findByStartedAtBetweenOrderByStartedAtDesc(start, end);
        List<ValveAction> valveActions = valveActionRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(start, end);
        List<Zone> zones = zoneRepository.findAll();
        List<SensorNode> nodes = nodeRepository.findAll();

        long criticalAlerts = alerts.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity())).count();
        long closeActions = valveActions.stream().filter(v -> "CLOSE".equalsIgnoreCase(v.getAction())).count();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 32, 32, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND_DARK);
        Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_PRIMARY);
        Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font th = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font td = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        Font statNum = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND_DARK);
        Font statLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        // Header Banner
        Paragraph title = new Paragraph("LeakSense Weekly Safety & Incident Report", h1);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph subtitle = new Paragraph("Reporting Period: " + time(start) + " to " + time(end)
                + "  |  Generated: " + time(LocalDateTime.now()), sub);
        subtitle.setSpacingAfter(14);
        document.add(subtitle);

        // KPI Summary Box (4 columns)
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(18);

        addKpiCell(kpiTable, String.valueOf(alerts.size()), "Total Alerts Raised", statNum, statLabel);
        addKpiCell(kpiTable, String.valueOf(criticalAlerts), "Critical Leaks", statNum, statLabel);
        addKpiCell(kpiTable, String.valueOf(alarms.size()), "Alarm Incidents", statNum, statLabel);
        addKpiCell(kpiTable, String.valueOf(closeActions), "Emergency Isolations", statNum, statLabel);
        document.add(kpiTable);

        // Section 1: Alarms
        document.add(new Paragraph("1. Critical Alarm Events (" + alarms.size() + ")", h2));
        document.add(new Paragraph(" "));
        if (alarms.isEmpty()) {
            document.add(new Paragraph("No critical alarm events occurred during this window. System operated within safe bounds.", td));
        } else {
            PdfPTable alarmTable = new PdfPTable(6);
            alarmTable.setWidthPercentage(100);
            alarmTable.setWidths(new float[]{1.5f, 2f, 2f, 2f, 2.5f, 2.5f});
            String[] headers = {"Node", "Zone", "Sensor", "Status", "Triggered At", "Silenced/Cleared"};
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, th));
                c.setBackgroundColor(BRAND_PRIMARY);
                c.setPadding(5);
                alarmTable.addCell(c);
            }
            int i = 0;
            for (AlarmEvent a : alarms) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ROW_ALT;
                addCell(alarmTable, a.getNode() != null ? a.getNode().getNodeCode() : "-", td, bg);
                addCell(alarmTable, a.getZone() != null ? a.getZone().getName() : "-", td, bg);
                addCell(alarmTable, text(a.getSensor()), td, bg);
                addCell(alarmTable, text(a.getStatus()), td, bg);
                addCell(alarmTable, time(a.getStartedAt()), td, bg);
                String outcome = a.getClearedAt() != null ? "Cleared: " + time(a.getClearedAt()) :
                        (a.getSilencedBy() != null ? "Silenced by " + a.getSilencedBy() : "Active");
                addCell(alarmTable, outcome, td, bg);
            }
            alarmTable.setHeaderRows(1);
            alarmTable.setSpacingAfter(16);
            document.add(alarmTable);
        }

        // Section 2: Valve Actions
        document.add(new Paragraph("2. Valve Actions & Isolations (" + valveActions.size() + ")", h2));
        document.add(new Paragraph(" "));
        if (valveActions.isEmpty()) {
            document.add(new Paragraph("No valve operations were performed during this period.", td));
        } else {
            PdfPTable valveTable = new PdfPTable(5);
            valveTable.setWidthPercentage(100);
            valveTable.setWidths(new float[]{1.5f, 2f, 2f, 4f, 2.5f});
            String[] vHeaders = {"Action", "Zone", "Triggered By", "Reason", "Executed At"};
            for (String h : vHeaders) {
                PdfPCell c = new PdfPCell(new Phrase(h, th));
                c.setBackgroundColor(BRAND_PRIMARY);
                c.setPadding(5);
                valveTable.addCell(c);
            }
            int i = 0;
            for (ValveAction v : valveActions) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ROW_ALT;
                addCell(valveTable, text(v.getAction()), td, bg);
                addCell(valveTable, v.getZone() != null ? v.getZone().getName() : "-", td, bg);
                addCell(valveTable, text(v.getTriggeredBy()), td, bg);
                addCell(valveTable, text(v.getReason()), td, bg);
                addCell(valveTable, time(v.getPerformedAt()), td, bg);
            }
            valveTable.setHeaderRows(1);
            valveTable.setSpacingAfter(16);
            document.add(valveTable);
        }

        // Section 3: Sensor Alerts (top 25)
        int alertCount = alerts.size();
        document.add(new Paragraph("3. Leak & Sensor Alerts (" + alertCount + ")", h2));
        document.add(new Paragraph(" "));
        if (alerts.isEmpty()) {
            document.add(new Paragraph("Zero alert incidents were recorded during this period.", td));
        } else {
            PdfPTable aTable = new PdfPTable(6);
            aTable.setWidthPercentage(100);
            aTable.setWidths(new float[]{3.5f, 1.5f, 1.5f, 2f, 2f, 2.5f});
            String[] aHeaders = {"Title", "Severity", "Level", "Zone", "Node", "Created At"};
            for (String h : aHeaders) {
                PdfPCell c = new PdfPCell(new Phrase(h, th));
                c.setBackgroundColor(BRAND_PRIMARY);
                c.setPadding(5);
                aTable.addCell(c);
            }
            int i = 0;
            for (Alert a : alerts.stream().limit(50).toList()) {
                Color bg = (i++ % 2 == 0) ? Color.WHITE : ROW_ALT;
                addCell(aTable, text(a.getTitle()), td, bg);
                addCell(aTable, text(a.getSeverity()), td, bg);
                addCell(aTable, text(a.getLevel()), td, bg);
                addCell(aTable, a.getZone() != null ? a.getZone().getName() : "-", td, bg);
                addCell(aTable, a.getNode() != null ? a.getNode().getNodeCode() : "-", td, bg);
                addCell(aTable, time(a.getCreatedAt()), td, bg);
            }
            aTable.setHeaderRows(1);
            aTable.setSpacingAfter(16);
            document.add(aTable);
        }

        // Section 4: Zone & Node Overview
        document.add(new Paragraph("4. Pipeline Zones Overview (" + zones.size() + ")", h2));
        document.add(new Paragraph(" "));
        PdfPTable zTable = new PdfPTable(3);
        zTable.setWidthPercentage(100);
        zTable.setWidths(new float[]{3f, 4f, 2f});
        String[] zHeaders = {"Zone Name", "Description", "Current Risk Level"};
        for (String h : zHeaders) {
            PdfPCell c = new PdfPCell(new Phrase(h, th));
            c.setBackgroundColor(BRAND_PRIMARY);
            c.setPadding(5);
            zTable.addCell(c);
        }
        int zi = 0;
        for (Zone z : zones) {
            Color bg = (zi++ % 2 == 0) ? Color.WHITE : ROW_ALT;
            addCell(zTable, text(z.getName()), td, bg);
            addCell(zTable, text(z.getDescription()), td, bg);
            addCell(zTable, text(z.getRiskLevel()), td, bg);
        }
        zTable.setHeaderRows(1);
        zTable.setSpacingAfter(16);
        document.add(zTable);

        // Section 5: Sensor Failures & Health Incidents
        List<Alert> sensorFaults = alerts.stream()
                .filter(a -> "FAULT".equalsIgnoreCase(a.getLevel()) || "DEVICE".equalsIgnoreCase(a.getLevel()))
                .toList();
        document.add(new Paragraph("5. Sensor Health & Hardware Fault Log (" + sensorFaults.size() + ")", h2));
        document.add(new Paragraph(" "));
        if (sensorFaults.isEmpty()) {
            document.add(new Paragraph("Zero hardware faults or sensor flat-lines detected in this interval.", td));
        } else {
            PdfPTable fTable = new PdfPTable(5);
            fTable.setWidthPercentage(100);
            fTable.setWidths(new float[]{2f, 2f, 2f, 3.5f, 2.5f});
            String[] fHeaders = {"Node", "Type", "Status", "Diagnostic Message", "Timestamp"};
            for (String h : fHeaders) {
                PdfPCell c = new PdfPCell(new Phrase(h, th));
                c.setBackgroundColor(BRAND_PRIMARY);
                c.setPadding(5);
                fTable.addCell(c);
            }
            int fi = 0;
            for (Alert f : sensorFaults.stream().limit(30).toList()) {
                Color bg = (fi++ % 2 == 0) ? Color.WHITE : ROW_ALT;
                addCell(fTable, f.getNode() != null ? f.getNode().getNodeCode() : "-", td, bg);
                addCell(fTable, text(f.getLevel()), td, bg);
                addCell(fTable, text(f.getStatus()), td, bg);
                addCell(fTable, text(f.getTitle()), td, bg);
                addCell(fTable, time(f.getCreatedAt()), td, bg);
            }
            fTable.setHeaderRows(1);
            fTable.setSpacingAfter(16);
            document.add(fTable);
        }

        document.close();
        return out.toByteArray();
    }

    private void addKpiCell(PdfPTable table, String value, String label, Font numFont, Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND_LIGHT);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pNum = new Paragraph(value, numFont);
        pNum.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pNum);

        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(4);
        table.addCell(cell);
    }
}