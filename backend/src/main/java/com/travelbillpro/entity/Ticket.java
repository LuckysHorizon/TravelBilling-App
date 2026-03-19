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

    @Column(name = "pnr_number", nullable = false, unique = true, length = 20)
    private String pnrNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 10)
    private TicketType ticketType;

    @Column(name = "passenger_name", nullable = false)
    private String passengerName;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(length = 100)
    private String origin;

    @Column(length = 100)
    private String destination;

    @Column(name = "operator_name", length = 150)
    private String operatorName;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "service_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceCharge;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cgst;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sgst;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
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
