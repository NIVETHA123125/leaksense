package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double methanePpm;

    private Double pressureKpa;

    private Double oxygenPercent;

    private Double riskScore;

    private LocalDateTime recordedAt;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private SensorNode node;
}