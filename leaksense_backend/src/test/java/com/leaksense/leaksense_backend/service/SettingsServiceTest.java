package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.dto.ThresholdRequest;
import com.leaksense.leaksense_backend.entity.ThresholdSetting;
import com.leaksense.leaksense_backend.repository.ThresholdSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private ThresholdSettingRepository repository;

    @Mock
    private com.leaksense.leaksense_backend.repository.ThresholdAuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SettingsService settingsService;

    private ThresholdRequest validRequest() {
        ThresholdRequest r = new ThresholdRequest();
        r.setMethaneWarningPpm(200.0);
        r.setMethaneLimitPpm(350.0);
        r.setPressureNormalKpa(460.0);
        r.setPressureToleranceKpa(40.0);
        r.setPressureLowWarningKpa(440.0);
        r.setPressureLowCriticalKpa(420.0);
        r.setPressureHighWarningKpa(480.0);
        r.setPressureHighCriticalKpa(500.0);
        r.setOxygenLowWarning(20.0);
        r.setMinSafeOxygen(19.5);
        r.setMethaneMinValid(0.0);
        r.setMethaneMaxValid(10000.0);
        r.setPressureMinValid(15.0);
        r.setPressureMaxValid(700.0);
        r.setOxygenMinValid(0.0);
        r.setOxygenMaxValid(25.0);
        r.setWarningScore(30.0);
        r.setCriticalScore(60.0);
        return r;
    }

    private ThresholdSetting existingSetting() {
        ThresholdSetting s = new ThresholdSetting();
        s.setMethaneWarningPpm(200.0);
        s.setMethaneLimitPpm(350.0);
        s.setPressureNormalKpa(460.0);
        s.setPressureToleranceKpa(40.0);
        s.setPressureLowWarningKpa(440.0);
        s.setPressureLowCriticalKpa(420.0);
        s.setPressureHighWarningKpa(480.0);
        s.setPressureHighCriticalKpa(500.0);
        s.setOxygenLowWarning(20.0);
        s.setMinSafeOxygen(19.5);
        s.setMethaneMinValid(0.0);
        s.setMethaneMaxValid(10000.0);
        s.setPressureMinValid(15.0);
        s.setPressureMaxValid(700.0);
        s.setOxygenMinValid(0.0);
        s.setOxygenMaxValid(25.0);
        s.setWarningScore(30.0);
        s.setCriticalScore(60.0);
        s.setUpdatedBy("SYSTEM");
        return s;
    }

    private void assertRejected(ThresholdRequest request, String message) {
        assertThatThrownBy(() -> settingsService.update(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("get() returns the saved row and does not create another one")
    void getReturnsExistingRow() {
        ThresholdSetting existing = existingSetting();
        when(repository.findAll()).thenReturn(List.of(existing));

        ThresholdSetting result = settingsService.get();

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("get() creates the default limits when the table is empty")
    void getCreatesDefaultsWhenEmpty() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(ThresholdSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        ThresholdSetting result = settingsService.get();

        assertThat(result.getMethaneLimitPpm()).isEqualTo(350.0);
        assertThat(result.getPressureNormalKpa()).isEqualTo(460.0);
        assertThat(result.getPressureToleranceKpa()).isEqualTo(40.0);
        assertThat(result.getMinSafeOxygen()).isEqualTo(19.5);
        assertThat(result.getWarningScore()).isEqualTo(30.0);
        assertThat(result.getCriticalScore()).isEqualTo(60.0);
        assertThat(result.getUpdatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("update() saves the new values and records who changed them")
    void updateSavesNewValues() {
        when(repository.findAll()).thenReturn(List.of(existingSetting()));
        when(repository.save(any(ThresholdSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        ThresholdRequest request = validRequest();
        request.setMethaneLimitPpm(200.0);
        request.setMethaneWarningPpm(100.0); // must be < methaneLimitPpm (200.0)

        ThresholdSetting result = settingsService.update(request, "admin");

        assertThat(result.getMethaneLimitPpm()).isEqualTo(200.0);
        assertThat(result.getUpdatedBy()).isEqualTo("admin");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 5000.0})
    @DisplayName("Methane limits exactly at the edges are accepted")
    void methaneEdgesAreAccepted(double value) {
        when(repository.findAll()).thenReturn(List.of(existingSetting()));
        when(repository.save(any(ThresholdSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        ThresholdRequest request = validRequest();
        request.setMethaneLimitPpm(value);
        request.setMethaneWarningPpm(Math.min(value / 2, 200.0));

        assertThat(settingsService.update(request, "admin").getMethaneLimitPpm()).isEqualTo(value);
    }

    @Test
    @DisplayName("A missing value is rejected")
    void missingValueIsRejected() {
        ThresholdRequest request = validRequest();
        request.setWarningScore(null);

        assertRejected(request, "All threshold values are required");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.9, 5000.1, 10000.0})
    @DisplayName("Methane limit outside 1 to 5000 ppm is rejected")
    void methaneOutOfRangeIsRejected(double value) {
        ThresholdRequest request = validRequest();
        request.setMethaneLimitPpm(value);

        assertRejected(request, "Methane limit must be between 1 and 5000 ppm");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -5.0})
    @DisplayName("Normal pressure of 0 or less is rejected")
    void normalPressureMustBePositive(double value) {
        ThresholdRequest request = validRequest();
        request.setPressureNormalKpa(value);

        assertRejected(request, "Normal pressure must be above 0 kPa");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0})
    @DisplayName("Pressure tolerance of 0 or less is rejected")
    void pressureToleranceMustBePositive(double value) {
        ThresholdRequest request = validRequest();
        request.setPressureToleranceKpa(value);

        assertRejected(request, "Pressure tolerance must be above 0 kPa");
    }

    @ParameterizedTest
    @ValueSource(doubles = {9.9, 20.9, 25.0})
    @DisplayName("Minimum safe oxygen outside 10 to 20.8 is rejected")
    void oxygenOutOfRangeIsRejected(double value) {
        ThresholdRequest request = validRequest();
        request.setMinSafeOxygen(value);

        assertRejected(request, "Minimum safe oxygen must be between 10 and 20.8 percent");
    }

    @Test
    @DisplayName("Warning score equal to the critical score is rejected")
    void warningEqualToCriticalIsRejected() {
        ThresholdRequest request = validRequest();
        request.setWarningScore(60.0);
        request.setCriticalScore(60.0);

        assertRejected(request, "Scores must satisfy 0 < warning < critical <= 100");
    }

    @Test
    @DisplayName("Warning score above the critical score is rejected")
    void warningAboveCriticalIsRejected() {
        ThresholdRequest request = validRequest();
        request.setWarningScore(80.0);
        request.setCriticalScore(60.0);

        assertRejected(request, "Scores must satisfy 0 < warning < critical <= 100");
    }

    @Test
    @DisplayName("A critical score above 100 is rejected")
    void criticalAbove100IsRejected() {
        ThresholdRequest request = validRequest();
        request.setCriticalScore(101.0);

        assertRejected(request, "Scores must satisfy 0 < warning < critical <= 100");
    }

    @Test
    @DisplayName("A warning score of 0 is rejected")
    void warningOfZeroIsRejected() {
        ThresholdRequest request = validRequest();
        request.setWarningScore(0.0);

        assertRejected(request, "Scores must satisfy 0 < warning < critical <= 100");
    }

    @Test
    @DisplayName("An old row without the new limits gets default values saved")
    void getFillsMissingLimitsOnOldRow() {
        ThresholdSetting oldRow = new ThresholdSetting();
        oldRow.setMethaneLimitPpm(350.0);
        oldRow.setPressureNormalKpa(460.0);
        oldRow.setPressureToleranceKpa(40.0);
        oldRow.setMinSafeOxygen(19.5);
        oldRow.setWarningScore(30.0);
        oldRow.setCriticalScore(60.0);
        when(repository.findAll()).thenReturn(List.of(oldRow));
        when(repository.save(any(ThresholdSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        ThresholdSetting result = settingsService.get();

        assertThat(result.getMethaneWarningPpm()).isEqualTo(200.0);
        assertThat(result.getPressureLowCriticalKpa()).isEqualTo(420.0);
        assertThat(result.getPressureHighCriticalKpa()).isEqualTo(500.0);
        assertThat(result.getOxygenLowWarning()).isEqualTo(20.0);
        assertThat(result.getPressureMaxValid()).isEqualTo(700.0);
        verify(repository).save(oldRow);
    }

    @Test
    @DisplayName("Methane warning at or above the critical limit is rejected")
    void methaneWarningMustBeBelowCritical() {
        ThresholdRequest request = validRequest();
        request.setMethaneWarningPpm(350.0);

        assertRejected(request,
                "Methane warning must be above 0 and below the critical methane limit");
    }

    @Test
    @DisplayName("Pressure limits in the wrong order are rejected")
    void pressureLimitsMustBeInOrder() {
        ThresholdRequest request = validRequest();
        request.setPressureLowWarningKpa(410.0);

        assertRejected(request,
                "Pressure limits must satisfy: low critical < low warning < normal < high warning < high critical");
    }

    @Test
    @DisplayName("Oxygen warning at or below the critical level is rejected")
    void oxygenWarningMustBeAboveCritical() {
        ThresholdRequest request = validRequest();
        request.setOxygenLowWarning(19.5);

        assertRejected(request,
                "Oxygen warning must be above the critical oxygen level and at most 20.8 percent");
    }

    @Test
    @DisplayName("A methane sensor range below the critical limit is rejected")
    void methaneSensorRangeMustCoverLimit() {
        ThresholdRequest request = validRequest();
        request.setMethaneMaxValid(300.0);

        assertRejected(request, "Methane sensor range must be valid and above the critical limit");
    }

    @Test
    @DisplayName("A pressure sensor range that excludes a limit is rejected")
    void pressureSensorRangeMustCoverLimits() {
        ThresholdRequest request = validRequest();
        request.setPressureMinValid(430.0);

        assertRejected(request, "Pressure sensor range must contain all the pressure limits");
    }

    @Test
    @DisplayName("An oxygen sensor range that excludes normal air is rejected")
    void oxygenSensorRangeMustCoverNormalAir() {
        ThresholdRequest request = validRequest();
        request.setOxygenMaxValid(20.0);

        assertRejected(request,
                "Oxygen sensor range must contain the oxygen limits and normal air (20.9 percent)");
    }


}
