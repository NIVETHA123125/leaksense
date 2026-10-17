package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.AiInsight;
import com.leaksense.leaksense_backend.dto.AiReportSummary;
import com.leaksense.leaksense_backend.dto.ChatRequest;
import com.leaksense.leaksense_backend.dto.ChatResponse;
import com.leaksense.leaksense_backend.service.AiChatService;
import com.leaksense.leaksense_backend.service.AiInsightService;
import com.leaksense.leaksense_backend.service.AiReportSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;
    private final AiReportSummaryService reportSummaryService;
    private final AiInsightService aiInsightService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody(required = false) ChatRequest request) {
        return aiChatService.processMessage(request);
    }

    @GetMapping("/report-summary")
    public AiReportSummary getReportSummary() {
        return reportSummaryService.generateSummary();
    }

    @GetMapping("/insights")
    public List<AiInsight> getInsights() {
        return aiInsightService.getInsights();
    }
}
