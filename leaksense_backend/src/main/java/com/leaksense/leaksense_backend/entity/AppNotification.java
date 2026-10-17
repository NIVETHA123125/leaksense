package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false, length = 50)
    private String type; // ALERT, ALARM, VALVE, NODE, SYSTEM

    @Column(nullable = false, length = 50)
    private String severity; // INFO, WARNING, CRITICAL

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long zoneId;

    private Long nodeId;
}
