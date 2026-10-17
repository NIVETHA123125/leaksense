package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.DemoRequest;
import com.leaksense.leaksense_backend.dto.LandingContentDto;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicService {

    private final SensorNodeRepository nodeRepository;
    private final ZoneRepository zoneRepository;
    private final AlertRepository alertRepository;

    public LandingContentDto getLandingContent() {
        return LandingContentDto.builder()
                .platformName("LeakSense IoT-based Gas Leak Detection System")
                .tagline("Smart Gas Leak Detection & Automated Safety System")
                .description("A smart IoT-based pipeline monitoring system that continuously tracks gas levels and pressure, detects leaks early, and automatically closes safety valves to protect people and property.")
                .stats(Map.of(
                        "latency", "< 1.2s",
                        "response", "Automatic Valve Closure",
                        "reliability", "99.9% Reliability",
                        "accuracy", "Multi-Sensor Tracking"
                ))
                .features(List.of(
                        LandingContentDto.FeatureItem.builder()
                                .id("sensor-telemetry")
                                .icon("activity")
                                .badge("Core IoT")
                                .title("High-Precision Multi-Sensor Telemetry")
                                .description("Continuous real-time sampling of combustible methane (MQ-4), line pressure (MPX5700), oxygen concentration (ME2-O2), and battery health.")
                                .build(),
                        LandingContentDto.FeatureItem.builder()
                                .id("autonomous-valve")
                                .icon("shield")
                                .badge("Safety Actuation")
                                .title("Autonomous & Remote Solenoid Isolation")
                                .description("Instant automated valve closure on threshold breach with full hardware manual override and fail-safe stuck detection.")
                                .build(),
                        LandingContentDto.FeatureItem.builder()
                                .id("ai-prediction")
                                .icon("cpu")
                                .badge("AI Intelligence")
                                .title("Predictive Early-Warning Engine")
                                .description("Evaluates derivative rates of change and cross-sensor drift to forecast leaks before explosive concentrations are reached.")
                                .build(),
                        LandingContentDto.FeatureItem.builder()
                                .id("omni-notify")
                                .icon("bell")
                                .badge("Omni-Channel")
                                .title("Multi-Channel Alert Dispatch")
                                .description("Instant WebSocket browser popups, in-app notification center, automated email briefings, and Twilio SMS for critical emergencies.")
                                .build(),
                        LandingContentDto.FeatureItem.builder()
                                .id("ai-assistant")
                                .icon("message-square")
                                .badge("Assistant")
                                .title("Operator Dashboard Chatbot")
                                .description("Natural language pipeline assistant answering questions on current risk, valve positions, battery levels, and safety protocols.")
                                .build(),
                        LandingContentDto.FeatureItem.builder()
                                .id("compliance-pdf")
                                .icon("file-text")
                                .badge("Auditing")
                                .title("Automated Weekly Safety Auditing")
                                .description("Scheduled executive PDF reports delivered directly to management with event logs, KPI cards, and compliance records.")
                                .build()
                ))
                .hardwareSpecs(List.of(
                        LandingContentDto.HardwareSpec.builder()
                                .component("Microcontroller")
                                .model("ESP32-WROOM-32 / LoRaWAN")
                                .range("2.4 GHz Wi-Fi / 868-915 MHz LoRa")
                                .role("Edge node sampling, local safety logic & transmission")
                                .build(),
                        LandingContentDto.HardwareSpec.builder()
                                .component("Methane Sensor")
                                .model("MQ-4 Semiconductor Gas Sensor")
                                .range("300 - 10,000 ppm CH4")
                                .role("Combustible gas & pre-combustion detection")
                                .build(),
                        LandingContentDto.HardwareSpec.builder()
                                .component("Pressure Transducer")
                                .model("MPX5700 Series Piezoresistive")
                                .range("0 - 700 kPa (0 - 101.5 psi)")
                                .role("Line pressure integrity & rupture detection")
                                .build(),
                        LandingContentDto.HardwareSpec.builder()
                                .component("Oxygen Sensor")
                                .model("ME2-O2 Electrochemical")
                                .range("0 - 25% Vol O2")
                                .role("Displacement monitoring & asphyxiation hazard alert")
                                .build(),
                        LandingContentDto.HardwareSpec.builder()
                                .component("Isolation Valve")
                                .model("12V/24V Solenoid Shutoff Actuator")
                                .range("Normally Closed / Direct Acting")
                                .role("Instant physical pipeline segment isolation")
                                .build()
                ))
                .faq(List.of(
                        LandingContentDto.FaqItem.builder()
                                .question("How fast does the system shut off the valve during a critical leak?")
                                .answer("Autonomous shutoff executes in under 800 milliseconds upon detecting a critical threshold breach. Remote dashboard commands dispatch in under 1.2 seconds over WebSockets.")
                                .build(),
                        LandingContentDto.FaqItem.builder()
                                .question("What happens if network connectivity is temporarily lost?")
                                .answer("Edge sensor nodes maintain local threshold evaluation logic and will autonomously trigger local solenoid cutoffs even when completely disconnected from the central server.")
                                .build(),
                        LandingContentDto.FaqItem.builder()
                                .question("Can LeakSense integrate with our existing SCADA or PLC network?")
                                .answer("Yes! LeakSense exposes clean REST APIs, WebSockets (STOMP), and configurable telemetry webhooks compatible with standard industrial IoT frameworks.")
                                .build()
                ))
                .build();
    }

    public Map<String, Object> getPublicSystemStatus() {
        long totalNodes = nodeRepository.count();
        long onlineNodes = nodeRepository.countByStatus("ONLINE");
        long totalZones = zoneRepository.count();
        long activeAlerts = alertRepository.countByStatusNot("RESOLVED");
        long criticalAlerts = alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED");

        String status = criticalAlerts > 0 ? "CRITICAL_HAZARD"
                : (activeAlerts > 0 ? "ELEVATED_RISK" : "OPTIMAL_SAFETY");

        return Map.of(
                "platform", "LeakSense Industrial IoT",
                "status", status,
                "totalZones", totalZones,
                "totalNodes", totalNodes,
                "onlineNodes", onlineNodes,
                "activeAlerts", activeAlerts,
                "systemHealthScore", criticalAlerts > 0 ? 60.0 : (activeAlerts > 0 ? 85.0 : 99.8)
        );
    }

    public Map<String, Object> submitDemoRequest(DemoRequest request) {
        log.info("Received Demo Request: Name={}, Email={}, Company={}",
                request.getName(), request.getEmail(), request.getCompany());
        return Map.of(
                "success", true,
                "message", "Thank you, " + (request.getName() != null ? request.getName() : "for your interest")
                        + "! The LeakSense team has received your demo request and will reach out shortly."
        );
    }
}
