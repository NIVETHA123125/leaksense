package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdEvaluatorTest {

    private final ThresholdEvaluator evaluator = new ThresholdEvaluator();
    private ThresholdSetting limits;

    @BeforeEach
    void setUp() {
        limits = new ThresholdSetting();
        limits.setMethaneWarningPpm(200.0);
        limits.setMethaneLimitPpm(350.0);
        limits.setPressureNormalKpa(460.0);
        limits.setPressureToleranceKpa(40.0);
        limits.setPressureLowWarningKpa(440.0);
        limits.setPressureLowCriticalKpa(420.0);
        limits.setPressureHighWarningKpa(480.0);
        limits.setPressureHighCriticalKpa(500.0);
        limits.setOxygenLowWarning(20.0);
        limits.setMinSafeOxygen(19.5);
        limits.setMethaneMinValid(0.0);
        limits.setMethaneMaxValid(10000.0);
        limits.setPressureMinValid(15.0);
        limits.setPressureMaxValid(700.0);
        limits.setOxygenMinValid(0.0);
        limits.setOxygenMaxValid(25.0);
        limits.setWarningScore(30.0);
        limits.setCriticalScore(60.0);
    }

    private ThresholdEvaluator.SensorCheck methane(double v) {
        return evaluator.evaluate(v, 460, 20.9, limits).methane();
    }

    private ThresholdEvaluator.SensorCheck pressure(double v) {
        return evaluator.evaluate(0, v, 20.9, limits).pressure();
    }

    private ThresholdEvaluator.SensorCheck oxygen(double v) {
        return evaluator.evaluate(0, 460, v, limits).oxygen();
    }

    @ParameterizedTest
    @CsvSource({
            "0,NORMAL,",
            "200,NORMAL,",
            "200.1,WARNING,HIGH",
            "350,WARNING,HIGH",
            "350.1,CRITICAL,HIGH",
            "10000,CRITICAL,HIGH",
            "10000.1,FAULT,HIGH",
            "-1,FAULT,LOW"
    })
    @DisplayName("Methane levels are classified correctly, including the edges")
    void methaneLevels(double value, ThresholdEvaluator.Status status, String direction) {
        ThresholdEvaluator.SensorCheck check = methane(value);

        assertThat(check.status()).isEqualTo(status);
        assertThat(check.direction()).isEqualTo(direction);
        assertThat(check.sensor()).isEqualTo("METHANE");
    }

    @ParameterizedTest
    @CsvSource({
            "460,NORMAL,",
            "440,NORMAL,",
            "439.9,WARNING,LOW",
            "420,WARNING,LOW",
            "419.9,CRITICAL,LOW",
            "15,CRITICAL,LOW",
            "14.9,FAULT,LOW",
            "480,NORMAL,",
            "480.1,WARNING,HIGH",
            "500,WARNING,HIGH",
            "500.1,CRITICAL,HIGH",
            "700,CRITICAL,HIGH",
            "700.1,FAULT,HIGH"
    })
    @DisplayName("Pressure levels are classified correctly in both directions")
    void pressureLevels(double value, ThresholdEvaluator.Status status, String direction) {
        ThresholdEvaluator.SensorCheck check = pressure(value);

        assertThat(check.status()).isEqualTo(status);
        assertThat(check.direction()).isEqualTo(direction);
        assertThat(check.sensor()).isEqualTo("PRESSURE");
    }

    @ParameterizedTest
    @CsvSource({
            "20.9,NORMAL,",
            "20,NORMAL,",
            "19.9,WARNING,LOW",
            "19.5,WARNING,LOW",
            "19.4,CRITICAL,LOW",
            "0,CRITICAL,LOW",
            "-0.1,FAULT,LOW",
            "25,NORMAL,",
            "25.1,FAULT,HIGH"
    })
    @DisplayName("Oxygen levels are classified correctly")
    void oxygenLevels(double value, ThresholdEvaluator.Status status, String direction) {
        ThresholdEvaluator.SensorCheck check = oxygen(value);

        assertThat(check.status()).isEqualTo(status);
        assertThat(check.direction()).isEqualTo(direction);
        assertThat(check.sensor()).isEqualTo("OXYGEN");
    }

    @Test
    @DisplayName("A warning reports the warning limit, a critical reports the critical limit")
    void reportsTheCrossedLimit() {
        assertThat(methane(250).limit()).isEqualTo(200.0);
        assertThat(methane(400).limit()).isEqualTo(350.0);
        assertThat(pressure(430).limit()).isEqualTo(440.0);
        assertThat(pressure(410).limit()).isEqualTo(420.0);
        assertThat(oxygen(19.8).limit()).isEqualTo(20.0);
        assertThat(oxygen(19.0).limit()).isEqualTo(19.5);
    }

    @Test
    @DisplayName("A sensor failure reports the valid-range limit that was broken")
    void faultReportsTheRangeLimit() {
        assertThat(pressure(5).limit()).isEqualTo(15.0);
        assertThat(pressure(800).limit()).isEqualTo(700.0);
        assertThat(methane(20000).limit()).isEqualTo(10000.0);
    }

    @Test
    @DisplayName("A check carries the label, unit and reading for messages")
    void checkCarriesDisplayDetails() {
        ThresholdEvaluator.SensorCheck check = methane(389.0);

        assertThat(check.label()).isEqualTo("Methane");
        assertThat(check.unit()).isEqualTo("ppm");
        assertThat(check.value()).isEqualTo(389.0);
    }

    @Test
    @DisplayName("Each sensor is judged on its own")
    void sensorsAreIndependent() {
        ThresholdEvaluator.Evaluation result = evaluator.evaluate(400, 5, 20.9, limits);

        assertThat(result.methane().status()).isEqualTo(ThresholdEvaluator.Status.CRITICAL);
        assertThat(result.pressure().status()).isEqualTo(ThresholdEvaluator.Status.FAULT);
        assertThat(result.oxygen().status()).isEqualTo(ThresholdEvaluator.Status.NORMAL);
        assertThat(result.all()).hasSize(3);
    }

    @Test
    @DisplayName("Changing a limit changes the verdict")
    void changingALimitChangesTheVerdict() {
        assertThat(methane(150).status()).isEqualTo(ThresholdEvaluator.Status.NORMAL);

        limits.setMethaneWarningPpm(100.0);

        assertThat(methane(150).status()).isEqualTo(ThresholdEvaluator.Status.WARNING);
    }
}
