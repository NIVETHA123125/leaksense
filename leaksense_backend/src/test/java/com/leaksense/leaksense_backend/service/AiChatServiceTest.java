package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.ChatRequest;
import com.leaksense.leaksense_backend.dto.ChatResponse;
import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlarmEventRepository alarmRepository;

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private ValveActionRepository valveActionRepository;

    @Mock
    private ReadingRepository readingRepository;

    @InjectMocks
    private AiChatService chatService;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");
    }

    @Test
    @DisplayName("Returns greeting and suggestions for empty message")
    void handlesEmptyGreeting() {
        ChatResponse resp = chatService.processMessage(new ChatRequest("", null));

        assertThat(resp.getIntent()).isEqualTo("GREETING");
        assertThat(resp.getReply()).contains("LeakSense Safety AI Assistant");
        assertThat(resp.getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("Handles alerts/leaks query when alerts are present")
    void handlesAlertsQueryWithAlerts() {
        Alert alert = new Alert(1L, "Methane Critical", "Leak detected", "SENSOR", "CRITICAL",
                "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");
        when(alertRepository.findByStatusNotOrderByCreatedAtDesc("RESOLVED")).thenReturn(List.of(alert));

        ChatResponse resp = chatService.processMessage(new ChatRequest("Are there any active leaks?", null));

        assertThat(resp.getIntent()).isEqualTo("ALERT_QUERY");
        assertThat(resp.getReply()).contains("1 active alert(s)").contains("CRITICAL");
    }

    @Test
    @DisplayName("Handles alerts/leaks query when no alerts exist")
    void handlesAlertsQueryEmpty() {
        when(alertRepository.findByStatusNotOrderByCreatedAtDesc("RESOLVED")).thenReturn(Collections.emptyList());

        ChatResponse resp = chatService.processMessage(new ChatRequest("check alerts", null));

        assertThat(resp.getIntent()).isEqualTo("ALERT_QUERY");
        assertThat(resp.getReply()).contains("no active alerts or leaks detected");
    }

    @Test
    @DisplayName("Handles alarms query")
    void handlesAlarmsQuery() {
        AlarmEvent alarm = new AlarmEvent(1L, node, zone, "methane", "Methane high", "ACTIVE",
                LocalDateTime.now(), null, null, null);
        when(alarmRepository.findByStatusInOrderByStartedAtDesc(any())).thenReturn(List.of(alarm));

        ChatResponse resp = chatService.processMessage(new ChatRequest("is the alarm sounding?", null));

        assertThat(resp.getIntent()).isEqualTo("ALARM_QUERY");
        assertThat(resp.getReply()).contains("alarm event(s) are currently open");
    }

    @Test
    @DisplayName("Handles valve status query")
    void handlesValvesQuery() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(valveActionRepository.findTop5ByOrderByPerformedAtDesc()).thenReturn(Collections.emptyList());

        ChatResponse resp = chatService.processMessage(new ChatRequest("what are the valve positions?", null));

        assertThat(resp.getIntent()).isEqualTo("VALVE_QUERY");
        assertThat(resp.getReply()).contains("Current Pipeline Valve Status").contains("Zone A");
    }

    @Test
    @DisplayName("Handles node diagnostics query (battery and offline)")
    void handlesNodesQuery() {
        SensorNode lowBatNode = new SensorNode(2L, "NODE-B1", "ONLINE", 12, LocalDateTime.now(), zone, "OPEN");
        when(nodeRepository.findAll()).thenReturn(List.of(node, lowBatNode));

        ChatResponse resp = chatService.processMessage(new ChatRequest("check node batteries", null));

        assertThat(resp.getIntent()).isEqualTo("NODE_QUERY");
        assertThat(resp.getReply()).contains("NODE-B1 (12%)");
    }

    @Test
    @DisplayName("Handles safety protocol query")
    void handlesProtocolQuery() {
        ChatResponse resp = chatService.processMessage(new ChatRequest("what is the protocol for methane leak?", null));

        assertThat(resp.getIntent()).isEqualTo("PROTOCOL_QUERY");
        assertThat(resp.getReply()).contains("Standard Emergency Response Protocol");
    }

    @Test
    @DisplayName("Handles system overview query")
    void handlesOverviewQuery() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(alertRepository.countByStatusNot("RESOLVED")).thenReturn(0L);
        when(alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED")).thenReturn(0L);
        when(nodeRepository.count()).thenReturn(1L);
        when(nodeRepository.countByStatus("ONLINE")).thenReturn(1L);

        ChatResponse resp = chatService.processMessage(new ChatRequest("give me a system overview", null));

        assertThat(resp.getIntent()).isEqualTo("OVERVIEW_QUERY");
        assertThat(resp.getReply()).contains("Pipeline System Overview").contains("OPTIMAL SAFETY");
    }
}
