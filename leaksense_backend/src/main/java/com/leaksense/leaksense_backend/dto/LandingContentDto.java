package com.leaksense.leaksense_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingContentDto {
    private String platformName;
    private String tagline;
    private String description;
    private Map<String, String> stats;
    private List<FeatureItem> features;
    private List<HardwareSpec> hardwareSpecs;
    private List<FaqItem> faq;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureItem {
        private String id;
        private String icon;
        private String title;
        private String description;
        private String badge;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HardwareSpec {
        private String component;
        private String model;
        private String range;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqItem {
        private String question;
        private String answer;
    }
}
