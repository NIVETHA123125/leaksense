package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 85, LocalDateTime.now(), zone, "OPEN");

        ReflectionTestUtils.setField(smsService, "enabled", false);
        ReflectionTestUtils.setField(smsService, "accountSid", "");
        ReflectionTestUtils.setField(smsService, "authToken", "");
        ReflectionTestUtils.setField(smsService, "fromNumber", "");
        ReflectionTestUtils.setField(smsService, "toNumbers", "");
    }

    @Nested
    @DisplayName("Alert SMS Tests")
    class AlertSmsTests {

        @Test
        @DisplayName("Does not send SMS for non-critical alert")
        void ignoresNonCriticalAlert() {
            Alert alert = new Alert(1L, "Methane Warning", "Methane is slightly high",
                    "SENSOR", "WARNING", "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");

            assertThatCode(() -> smsService.sendAlertSms(alert))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Handles critical alert when disabled gracefully")
        void handlesCriticalAlertWhenDisabled() {
            Alert alert = new Alert(1L, "Methane Critical", "Explosion hazard detected!",
                    "SENSOR", "CRITICAL", "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");

            assertThatCode(() -> smsService.sendAlertSms(alert))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Handles null alert safely")
        void handlesNullAlert() {
            assertThatCode(() -> smsService.sendAlertSms(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Alarm SMS Tests")
    class AlarmSmsTests {

        @Test
        @DisplayName("Handles alarm event when disabled gracefully")
        void handlesAlarmEventWhenDisabled() {
            AlarmEvent alarm = new AlarmEvent(1L, node, zone, "methane",
                    "Methane at 3.2% (limit 2.0%)", "ACTIVE", LocalDateTime.now(), null, null, null);

            assertThatCode(() -> smsService.sendAlarmSms(alarm))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Handles null alarm safely")
        void handlesNullAlarm() {
            assertThatCode(() -> smsService.sendAlarmSms(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Dispatch & Configuration Tests")
    class DispatchTests {

        @Test
        @DisplayName("Gracefully handles missing configuration without throwing")
        void handlesMissingConfig() {
            ReflectionTestUtils.setField(smsService, "enabled", true);
            ReflectionTestUtils.setField(smsService, "accountSid", "AC123");
            // authToken missing

            assertThatCode(() -> smsService.send("Test SMS Message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Gracefully catches network or twilio failures without crashing caller")
        void handlesDispatchFailureGracefully() {
            ReflectionTestUtils.setField(smsService, "enabled", true);
            ReflectionTestUtils.setField(smsService, "accountSid", "AC_DUMMY_SID");
            ReflectionTestUtils.setField(smsService, "authToken", "DUMMY_TOKEN");
            ReflectionTestUtils.setField(smsService, "fromNumber", "+1234567890");
            ReflectionTestUtils.setField(smsService, "toNumbers", "+0987654321");

            // Should not throw even though network endpoint will fail or return 401
            assertThatCode(() -> smsService.send("Immediate alert!"))
                    .doesNotThrowAnyException();
        }
    }
}
