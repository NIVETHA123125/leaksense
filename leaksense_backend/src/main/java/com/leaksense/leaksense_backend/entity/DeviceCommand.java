package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "node_id")
    private SensorNode node;

    private String type;

    private String status;

    private String source;

    private String requestedBy;

    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private LocalDateTime ackedAt;

    @Column(length = 500)
    private String resultNote;
}
