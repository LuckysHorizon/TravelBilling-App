package com.travelbillpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gst_config")
@Getter
@Setter
public class GstConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "sgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_by")
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
