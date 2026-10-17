package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskScoreServiceTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private RiskScoreService riskScoreService;

    private ThresholdSetting settings;

    @BeforeEach
    void setUp() {
        settings = new ThresholdSetting();
        settings.setMethaneLimitPpm(350.0);
        settings.setPressureNormalKpa(460.0);
        settings.setPressureToleranceKpa(40.0);
        settings.setMinSafeOxygen(19.5);
        settings.setWarningScore(30.0);
        settings.setCriticalScore(60.0);

        when(settingsService.get()).thenReturn(settings);
    }

    @Test
    @DisplayName("Normal conditions give a score of 0 and SAFE")
    void normalConditionsAreSafe() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(0, 460, 20.9);

        assertThat(result.score()).isCloseTo(0.0, within(0.01));
        assertThat(result.level()).isEqualTo("SAFE");
    }

    @Test
    @DisplayName("Methane exactly at the limit gives 60 points and CRITICAL")
    void methaneAtLimitIsCritical() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(350, 460, 20.9);

        assertThat(result.score()).isCloseTo(60.0, within(0.01));
        assertThat(result.level()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Half the methane limit gives exactly the warning score")
    void halfMethaneLimitIsWarning() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(175, 460, 20.9);

        assertThat(result.score()).isCloseTo(30.0, within(0.01));
        assertThat(result.level()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("Just below the warning score stays SAFE")
    void justBelowWarningIsSafe() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(170, 460, 20.9);

        assertThat(result.score()).isCloseTo(29.1, within(0.01));
        assertThat(result.level()).isEqualTo("SAFE");
    }

    @Test
    @DisplayName("Pressure drift beyond the tolerance is capped at 25 points")
    void pressureDriftIsCapped() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(0, 300, 20.9);

        assertThat(result.score()).isCloseTo(25.0, within(0.01));
        assertThat(result.level()).isEqualTo("SAFE");
    }

    @Test
    @DisplayName("Oxygen at the minimum safe level adds 15 points")
    void lowOxygenAddsPoints() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(0, 460, 19.5);

        assertThat(result.score()).isCloseTo(15.0, within(0.01));
    }

    @Test
    @DisplayName("Oxygen above normal never reduces the score")
    void highOxygenAddsNothing() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(0, 460, 22.0);

        assertThat(result.score()).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("The worst case is capped at 100")
    void worstCaseIsCappedAt100() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(5000, 0, 10);

        assertThat(result.score()).isCloseTo(100.0, within(0.01));
        assertThat(result.level()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Methane, pressure and oxygen add up together")
    void combinedReadingAddsUp() {
        RiskScoreService.RiskResult result = riskScoreService.calculate(175, 440, 20.2);

        assertThat(result.score()).isCloseTo(50.0, within(0.1));
        assertThat(result.level()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("Lowering the methane limit raises the score")
    void loweringMethaneLimitRaisesScore() {
        settings.setMethaneLimitPpm(200.0);

        RiskScoreService.RiskResult result = riskScoreService.calculate(175, 460, 20.9);

        assertThat(result.score()).isCloseTo(52.5, within(0.01));
        assertThat(result.level()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("Lowering the critical score changes the level")
    void loweringCriticalScoreChangesLevel() {
        settings.setMethaneLimitPpm(200.0);
        settings.setCriticalScore(50.0);

        RiskScoreService.RiskResult result = riskScoreService.calculate(175, 460, 20.9);

        assertThat(result.level()).isEqualTo("CRITICAL");
    }
}
