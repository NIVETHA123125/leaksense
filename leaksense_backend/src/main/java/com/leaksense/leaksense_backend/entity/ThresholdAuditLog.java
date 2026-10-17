package com.leaksense.leaksense_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "threshold_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parameterName;
    private Double oldValue;
    private Double newValue;
    private String changedBy;
    private LocalDateTime changedAt;
}
