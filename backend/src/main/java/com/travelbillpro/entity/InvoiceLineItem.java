package com.travelbillpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
@Getter
@Setter
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(nullable = false)
    private String particulars;

    @Column(name = "sac_code", length = 10)
    private String sacCode;

    @Column(name = "taxable_value", precision = 10, scale = 2)
    private BigDecimal taxableValue = BigDecimal.ZERO;

    @Column(name = "non_taxable_value", precision = 10, scale = 2)
    private BigDecimal nonTaxableValue = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "is_manually_added")
    private Boolean isManuallyAdded = false;
}
