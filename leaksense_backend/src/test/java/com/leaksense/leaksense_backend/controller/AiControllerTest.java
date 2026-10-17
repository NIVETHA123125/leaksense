package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.AiInsight;
import com.leaksense.leaksense_backend.dto.AiReportSummary;
import com.leaksense.leaksense_backend.dto.ChatRequest;
import com.leaksense.leaksense_backend.dto.ChatResponse;
import com.leaksense.leaksense_backend.service.AiChatService;
import com.leaksense.leaksense_backend.service.AiInsightService;
import com.leaksense.leaksense_backend.service.AiReportSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiChatService aiChatService;

    @Mock
    private AiReportSummaryService reportSummaryService;

    @Mock
    private AiInsightService aiInsightService;

    @InjectMocks
    private AiController aiController;

    @Test
    @DisplayName("chat endpoint calls aiChatService and returns response")
    void chatEndpoint() {
        ChatResponse expected = ChatResponse.builder()
                .reply("Status nominal")
                .intent("STATUS")
                .build();
        when(aiChatService.processMessage(any())).thenReturn(expected);

        ChatResponse response = aiController.chat(new ChatRequest("status", null));

        assertThat(response.getReply()).isEqualTo("Status nominal");
        verify(aiChatService).processMessage(any());
    }

    @Test
    @DisplayName("reportSummary endpoint calls reportSummaryService and returns summary")
    void reportSummaryEndpoint() {
        AiReportSummary expected = AiReportSummary.builder()
                .overallSafetyScore(95.0)
                .riskLevel("OPTIMAL")
                .build();
        when(reportSummaryService.generateSummary()).thenReturn(expected);

        AiReportSummary response = aiController.getReportSummary();

        assertThat(response.getOverallSafetyScore()).isEqualTo(95.0);
        verify(reportSummaryService).generateSummary();
    }

    @Test
    @DisplayName("insights endpoint calls aiInsightService and returns insights list")
    void insightsEndpoint() {
        AiInsight insight = AiInsight.builder()
                .id("EW-1")
                .title("Early warning")
                .detectedAt(LocalDateTime.now())
                .build();
        when(aiInsightService.getInsights()).thenReturn(List.of(insight));

        List<AiInsight> response = aiController.getInsights();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Early warning");
        verify(aiInsightService).getInsights();
    }
}
