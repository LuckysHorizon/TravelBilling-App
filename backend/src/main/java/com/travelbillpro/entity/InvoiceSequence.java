package com.travelbillpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_sequences")
@Getter
@Setter
public class InvoiceSequence {

    @Id
    @Column(name = "financial_year", length = 7)
    private String financialYear;

    @Column(name = "next_value")
    private Long nextValue = 1L;
}
