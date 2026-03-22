package com.travelbillpro.entity;

import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "pnr_number", length = 20)
    private String pnrNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 10)
    private TicketType ticketType;

    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    @Column(length = 100)
    private String origin;

    @Column(length = 100)
    private String destination;

    @Column(name = "operator_name", length = 150)
    private String operatorName;

    @Column(name = "base_fare", precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "service_charge", precision = 10, scale = 2)
    private BigDecimal serviceCharge;

    @Column(name = "passenger_service_fee", precision = 10, scale = 2)
    private BigDecimal passengerServiceFee;

    @Column(name = "user_development_charges", precision = 10, scale = 2)
    private BigDecimal userDevelopmentCharges;

    @Column(name = "agent_service_charges", precision = 10, scale = 2)
    private BigDecimal agentServiceCharges;

    @Column(name = "other_charges", precision = 10, scale = 2)
    private BigDecimal otherCharges;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "sac_code_air", length = 10)
    private String sacCodeAir;

    @Column(name = "sac_code_agent", length = 10)
    private String sacCodeAgent;

    @Column(precision = 10, scale = 2)
    private BigDecimal cgst;

    @Column(precision = 10, scale = 2)
    private BigDecimal sgst;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_panel_id")
    private BillingPanel billingPanel;

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
