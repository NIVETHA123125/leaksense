package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.DemoRequest;
import com.leaksense.leaksense_backend.dto.LandingContentDto;
import com.leaksense.leaksense_backend.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @GetMapping("/landing")
    public LandingContentDto getLandingContent() {
        return publicService.getLandingContent();
    }

    @GetMapping("/status")
    public Map<String, Object> getSystemStatus() {
        return publicService.getPublicSystemStatus();
    }

    @PostMapping("/demo-request")
    public Map<String, Object> submitDemoRequest(@RequestBody DemoRequest request) {
        return publicService.submitDemoRequest(request);
    }
}
