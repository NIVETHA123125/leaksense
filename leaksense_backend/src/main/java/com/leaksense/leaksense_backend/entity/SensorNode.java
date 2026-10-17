package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nodeCode;

    private String status;

    private Integer batteryLevel;

    private LocalDateTime lastSeen;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private Zone zone;

    private String valveState;

    public SensorNode(Long id, String nodeCode, String status, Integer batteryLevel,
                      LocalDateTime lastSeen, Zone zone) {
        this(id, nodeCode, status, batteryLevel, lastSeen, zone, "OPEN");
    }
}