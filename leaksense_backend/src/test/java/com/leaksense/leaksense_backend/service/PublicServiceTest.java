package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.DemoRequest;
import com.leaksense.leaksense_backend.dto.LandingContentDto;
import com.leaksense.leaksense_backend.repository.AlertRepository;
import com.leaksense.leaksense_backend.repository.SensorNodeRepository;
import com.leaksense.leaksense_backend.repository.ZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicServiceTest {

    @Mock
    private SensorNodeRepository nodeRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private PublicService publicService;

    @Test
    @DisplayName("getLandingContent returns rich structured marketing content")
    void getLandingContent() {
        LandingContentDto content = publicService.getLandingContent();

        assertThat(content).isNotNull();
        assertThat(content.getPlatformName()).isEqualTo("LeakSense IoT-based Gas Leak Detection System");
        assertThat(content.getFeatures()).hasSize(6);
        assertThat(content.getHardwareSpecs()).hasSize(5);
        assertThat(content.getFaq()).isNotEmpty();
    }

    @Test
    @DisplayName("getPublicSystemStatus returns live statistics")
    void getPublicSystemStatus() {
        when(nodeRepository.count()).thenReturn(5L);
        when(nodeRepository.countByStatus("ONLINE")).thenReturn(4L);
        when(zoneRepository.count()).thenReturn(2L);
        when(alertRepository.countByStatusNot("RESOLVED")).thenReturn(0L);
        when(alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED")).thenReturn(0L);

        Map<String, Object> status = publicService.getPublicSystemStatus();

        assertThat(status).containsEntry("status", "OPTIMAL_SAFETY");
        assertThat(status).containsEntry("totalNodes", 5L);
        assertThat(status).containsEntry("onlineNodes", 4L);
    }

    @Test
    @DisplayName("submitDemoRequest accepts and acknowledges request")
    void submitDemoRequest() {
        DemoRequest req = DemoRequest.builder()
                .name("Alice")
                .email("alice@energy.com")
                .company("EnergyCo")
                .message("Need evaluation")
                .build();

        Map<String, Object> resp = publicService.submitDemoRequest(req);

        assertThat(resp).containsEntry("success", true);
        assertThat(resp.get("message").toString()).contains("Alice");
    }
}
