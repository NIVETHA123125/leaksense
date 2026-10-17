package com.leaksense.leaksense_backend.controller;

import com.leaksense.leaksense_backend.entity.AppNotification;
import com.leaksense.leaksense_backend.repository.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class NotificationController {

    private final AppNotificationRepository notificationRepository;

    @GetMapping
    public List<AppNotification> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        }
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("unreadCount", notificationRepository.countByIsReadFalse());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<AppNotification> markAsRead(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setRead(true);
                    return ResponseEntity.ok(notificationRepository.save(n));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/mark-all-read")
    public Map<String, Integer> markAllAsRead() {
        int updated = notificationRepository.markAllAsRead();
        return Map.of("updated", updated);
    }
}
