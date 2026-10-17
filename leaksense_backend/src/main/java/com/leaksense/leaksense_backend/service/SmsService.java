package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Service
public class SmsService {

    @Value("${leaksense.sms.enabled:false}")
    private boolean enabled;

    @Value("${leaksense.sms.account-sid:}")
    private String accountSid;

    @Value("${leaksense.sms.auth-token:}")
    private String authToken;

    @Value("${leaksense.sms.from-number:}")
    private String fromNumber;

    @Value("${leaksense.sms.to-numbers:}")
    private String toNumbers;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Async
    public void sendAlertSms(Alert alert) {
        if (alert == null || !"CRITICAL".equalsIgnoreCase(alert.getSeverity())) {
            return;
        }
        String zone = alert.getZone() != null ? alert.getZone().getName() : "-";
        String node = alert.getNode() != null ? alert.getNode().getNodeCode() : "-";
        String text = String.format("[LeakSense CRITICAL] %s in %s (Node %s): %s",
                alert.getTitle(), zone, node, alert.getMessage());
        send(text);
    }

    @Async
    public void sendAlarmSms(AlarmEvent alarm) {
        if (alarm == null) {
            return;
        }
        String zone = alarm.getZone() != null ? alarm.getZone().getName() : "-";
        String node = alarm.getNode() != null ? alarm.getNode().getNodeCode() : "-";
        String text = String.format("[LeakSense ALARM] CRITICAL ALARM active in %s (Node %s, Sensor: %s): %s",
                zone, node, alarm.getSensor(), alarm.getMessage());
        send(text);
    }

    @Async
    public void send(String message) {
        if (!enabled || accountSid.isBlank() || authToken.isBlank() || toNumbers.isBlank()) {
            log.info("[SMS-SIMULATED] Recipients: [{}], Body: {}",
                    toNumbers.isBlank() ? "NOT_CONFIGURED" : toNumbers, message);
            return;
        }

        String[] recipients = toNumbers.split("\\s*,\\s*");
        for (String to : recipients) {
            if (to.isBlank()) continue;
            sendViaTwilio(to.trim(), message);
        }
    }

    private void sendViaTwilio(String to, String message) {
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid.trim());
            String auth = Base64.getEncoder().encodeToString(
                    (accountSid.trim() + ":" + authToken.trim()).getBytes(StandardCharsets.UTF_8));

            String body = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS successfully dispatched to {}: status {}", to, response.statusCode());
            } else {
                log.warn("Twilio SMS returned error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch SMS to {}: {}", to, e.getMessage());
        }
    }
}
