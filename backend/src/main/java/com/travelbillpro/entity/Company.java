package com.travelbillpro.entity;

import com.travelbillpro.enums.BillingCycle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "gst_number", nullable = false, unique = true, length = 15)
    private String gstNumber;

    @Column(name = "billing_email", nullable = false)
    private String billingEmail;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "service_charge_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceChargePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "contact_name")
    private String contactName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "pdf_storage_path", columnDefinition = "TEXT")
    private String pdfStoragePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
