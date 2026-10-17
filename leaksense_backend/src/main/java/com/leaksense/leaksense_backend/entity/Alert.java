package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String message;

    private String level;

    private String severity;

    private String status;

    private String acknowledgedBy;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private SensorNode node;

    private String sensor;

    public Alert(Long id, String title, String message, String level, String severity,
                 String status, String acknowledgedBy, LocalDateTime createdAt,
                 LocalDateTime resolvedAt, Zone zone, SensorNode node) {
        this(id, title, message, level, severity, status, acknowledgedBy,
                createdAt, resolvedAt, zone, node, null);
    }
}