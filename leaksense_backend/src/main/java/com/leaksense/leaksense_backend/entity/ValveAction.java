package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "valve_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    private String triggeredBy;

    private String reason;

    private String ipAddress;

    private LocalDateTime performedAt;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;
}
