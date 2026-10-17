package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.dto.DemoRequest;
import com.leaksense.leaksense_backend.dto.LandingContentDto;
import com.leaksense.leaksense_backend.service.PublicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private PublicService publicService;

    @InjectMocks
    private PublicController publicController;

    @Test
    @DisplayName("landing content endpoint calls publicService")
    void getLandingContent() {
        LandingContentDto dto = LandingContentDto.builder()
                .platformName("LeakSense IoT")
                .build();
        when(publicService.getLandingContent()).thenReturn(dto);

        LandingContentDto result = publicController.getLandingContent();

        assertThat(result.getPlatformName()).isEqualTo("LeakSense IoT");
        verify(publicService).getLandingContent();
    }

    @Test
    @DisplayName("status endpoint calls publicService")
    void getSystemStatus() {
        Map<String, Object> status = Map.of("status", "OPTIMAL");
        when(publicService.getPublicSystemStatus()).thenReturn(status);

        Map<String, Object> result = publicController.getSystemStatus();

        assertThat(result).containsEntry("status", "OPTIMAL");
        verify(publicService).getPublicSystemStatus();
    }

    @Test
    @DisplayName("demo request endpoint calls publicService")
    void submitDemoRequest() {
        DemoRequest req = DemoRequest.builder().name("Bob").build();
        when(publicService.submitDemoRequest(any())).thenReturn(Map.of("success", true));

        Map<String, Object> result = publicController.submitDemoRequest(req);

        assertThat(result).containsEntry("success", true);
        verify(publicService).submitDemoRequest(req);
    }
}
