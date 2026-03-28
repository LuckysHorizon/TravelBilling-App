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

    /** Flat ₹ CGST per ticket */
    @Column(name = "cgst_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal cgstRate;

    /** Flat ₹ SGST per ticket */
    @Column(name = "sgst_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal sgstRate;

    /** Flat ₹ service charge per ticket */
    @Column(name = "service_charge_per_ticket", precision = 10, scale = 2)
    private BigDecimal serviceChargePerTicket;

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
