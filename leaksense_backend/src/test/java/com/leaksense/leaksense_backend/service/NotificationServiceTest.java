package com.leaksense.leaksense_backend.service;

import com.leaksense.leaksense_backend.entity.AlarmEvent;
import com.leaksense.leaksense_backend.entity.Alert;
import com.leaksense.leaksense_backend.entity.AppNotification;
import com.leaksense.leaksense_backend.entity.SensorNode;
import com.leaksense.leaksense_backend.entity.ValveAction;
import com.leaksense.leaksense_backend.entity.Zone;
import com.leaksense.leaksense_backend.repository.AppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private NotificationService notificationService;

    private Zone zone;
    private SensorNode node;

    @BeforeEach
    void setUp() {
        zone = new Zone(1L, "Zone A", "Main pipeline", "SAFE");
        node = new SensorNode(1L, "NODE-A1", "ONLINE", 90, LocalDateTime.now(), zone, "OPEN");

        ReflectionTestUtils.setField(notificationService, "enabled", true);
        ReflectionTestUtils.setField(notificationService, "recipients", "admin@leaksense.com");
        ReflectionTestUtils.setField(notificationService, "from", "system@leaksense.com");
    }

    @Nested
    @DisplayName("In-App Notification Tests")
    class InAppNotificationTests {

        @Test
        @DisplayName("createInAppNotification persists entity and broadcasts to websocket")
        void createsAndBroadcastsNotification() {
            when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
                AppNotification n = invocation.getArgument(0);
                n.setId(10L);
                return n;
            });

            AppNotification created = notificationService.createInAppNotification(
                    "Test Title", "Test Message", "ALERT", "CRITICAL", 1L, 2L);

            assertThat(created).isNotNull();
            assertThat(created.getId()).isEqualTo(10L);
            assertThat(created.getTitle()).isEqualTo("Test Title");
            assertThat(created.getMessage()).isEqualTo("Test Message");
            assertThat(created.getType()).isEqualTo("ALERT");
            assertThat(created.getSeverity()).isEqualTo("CRITICAL");
            assertThat(created.isRead()).isFalse();

            verify(notificationRepository).save(any(AppNotification.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), eq(created));
        }
    }

    @Nested
    @DisplayName("Alert Notification Tests")
    class AlertNotificationTests {

        @Test
        @DisplayName("Critical alert triggers in-app notification, SMS, and email")
        void criticalAlertTriggersAllChannels() {
            Alert alert = new Alert(1L, "Methane Critical", "Explosion hazard!",
                    "SENSOR", "CRITICAL", "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");

            notificationService.sendAlertEmail(alert);

            verify(notificationRepository).save(any(AppNotification.class));
            verify(smsService).sendAlertSms(alert);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getSubject()).contains("CRITICAL").contains("Methane Critical");
            assertThat(sent.getText()).contains("Explosion hazard!");
        }

        @Test
        @DisplayName("Warning alert does not trigger SMS")
        void warningAlertDoesNotTriggerSms() {
            Alert alert = new Alert(1L, "Pressure Warning", "Pressure drop",
                    "SENSOR", "WARNING", "OPEN", null, LocalDateTime.now(), null, zone, node, "pressure");

            notificationService.sendAlertEmail(alert);

            verify(notificationRepository).save(any(AppNotification.class));
            verify(smsService, never()).sendAlertSms(any());
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Does not send email when email sending is disabled")
        void skipsEmailWhenDisabled() {
            ReflectionTestUtils.setField(notificationService, "enabled", false);

            Alert alert = new Alert(1L, "Alert Title", "Details",
                    "SENSOR", "WARNING", "OPEN", null, LocalDateTime.now(), null, zone, node, "methane");

            notificationService.sendAlertEmail(alert);

            verify(notificationRepository).save(any(AppNotification.class));
            verifyNoInteractions(mailSender);
        }
    }

    @Nested
    @DisplayName("Alarm Notification Tests")
    class AlarmNotificationTests {

        @Test
        @DisplayName("sendAlarmTriggered creates in-app notification, triggers SMS, and sends email")
        void alarmTriggeredDispatchesAllChannels() {
            AlarmEvent alarm = new AlarmEvent(1L, node, zone, "methane",
                    "Methane at 3.5%", "ACTIVE", LocalDateTime.now(), null, null, null);

            notificationService.sendAlarmTriggered(alarm);

            verify(notificationRepository).save(any(AppNotification.class));
            verify(smsService).sendAlarmSms(alarm);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("CRITICAL ALARM");
        }

        @Test
        @DisplayName("sendAlarmSilenced creates in-app notification and sends email with silencing details")
        void alarmSilencedDispatchesEmailAndNotification() {
            AlarmEvent alarm = new AlarmEvent(1L, node, zone, "methane",
                    "Methane at 3.5%", "SILENCED", LocalDateTime.now(), LocalDateTime.now(), "operator1", null);

            notificationService.sendAlarmSilenced(alarm, "operator1");

            verify(notificationRepository).save(any(AppNotification.class));
            verify(mailSender).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("sendAlarmCleared creates in-app notification and sends email with cleared details")
        void alarmClearedDispatchesEmailAndNotification() {
            AlarmEvent alarm = new AlarmEvent(1L, node, zone, "methane",
                    "Methane normal", "CLEARED", LocalDateTime.now(), null, null, LocalDateTime.now());

            notificationService.sendAlarmCleared(alarm);

            verify(notificationRepository).save(any(AppNotification.class));
            verify(mailSender).send(any(SimpleMailMessage.class));
        }
    }

    @Nested
    @DisplayName("Node Restored & Valve Action Tests")
    class LifecycleNotificationTests {

        @Test
        @DisplayName("sendNodeRestored creates in-app notification and sends email")
        void nodeRestoredDispatchesNotifications() {
            notificationService.sendNodeRestored(node);

            verify(notificationRepository).save(any(AppNotification.class));
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("Node Restored: NODE-A1");
        }

        @Test
        @DisplayName("sendValveAction creates in-app notification and sends email")
        void valveActionDispatchesNotifications() {
            ValveAction action = new ValveAction(1L, "CLOSE", "admin", "Emergency isolation",
                    "127.0.0.1", LocalDateTime.now(), zone);

            notificationService.sendValveAction(action);

            verify(notificationRepository).save(any(AppNotification.class));
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("Valve CLOSE");
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {

        @Test
        @DisplayName("Handles null objects gracefully without throwing")
        void handlesNulls() {
            notificationService.sendAlertEmail(null);
            notificationService.sendAlarmTriggered(null);
            notificationService.sendAlarmSilenced(null, "user");
            notificationService.sendAlarmCleared(null);
            notificationService.sendNodeRestored(null);
            notificationService.sendValveAction(null);

            verifyNoInteractions(mailSender, smsService);
        }
    }
}
