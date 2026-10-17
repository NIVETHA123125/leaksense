package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alarm_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private SensorNode node;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    private String sensor;

    @Column(length = 500)
    private String message;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime silencedAt;

    private String silencedBy;

    private LocalDateTime clearedAt;
}
