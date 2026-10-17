package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.AppNotification;
import com.leaksense.leaksense_backend.repository.AppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private AppNotificationRepository notificationRepository;

    @InjectMocks
    private NotificationController controller;

    private AppNotification n1;
    private AppNotification n2;

    @BeforeEach
    void setUp() {
        n1 = AppNotification.builder()
                .id(1L)
                .title("Alert 1")
                .message("High methane")
                .type("ALERT")
                .severity("CRITICAL")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        n2 = AppNotification.builder()
                .id(2L)
                .title("Alert 2")
                .message("Low battery")
                .type("NODE")
                .severity("WARNING")
                .isRead(true)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    @DisplayName("getNotifications returns recent notifications when unreadOnly is false")
    void getNotificationsAll() {
        when(notificationRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(n1, n2));

        List<AppNotification> result = controller.getNotifications(false);

        assertThat(result).hasSize(2);
        verify(notificationRepository).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("getNotifications returns unread only when unreadOnly is true")
    void getNotificationsUnreadOnly() {
        when(notificationRepository.findByIsReadFalseOrderByCreatedAtDesc()).thenReturn(List.of(n1));

        List<AppNotification> result = controller.getNotifications(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(notificationRepository).findByIsReadFalseOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("getUnreadCount returns correct map with count")
    void getUnreadCount() {
        when(notificationRepository.countByIsReadFalse()).thenReturn(5L);

        Map<String, Long> countMap = controller.getUnreadCount();

        assertThat(countMap).containsEntry("unreadCount", 5L);
        verify(notificationRepository).countByIsReadFalse();
    }

    @Test
    @DisplayName("markAsRead marks notification as read when found")
    void markAsReadSuccess() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n1));
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<AppNotification> response = controller.markAsRead(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isRead()).isTrue();
        verify(notificationRepository).save(n1);
    }

    @Test
    @DisplayName("markAsRead returns 404 when notification does not exist")
    void markAsReadNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<AppNotification> response = controller.markAsRead(999L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAllAsRead updates all unread notifications")
    void markAllAsRead() {
        when(notificationRepository.markAllAsRead()).thenReturn(3);

        Map<String, Integer> result = controller.markAllAsRead();

        assertThat(result).containsEntry("updated", 3);
        verify(notificationRepository).markAllAsRead();
    }
}
