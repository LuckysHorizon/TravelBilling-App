package com.travelbillpro.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class EmployeeBillingDto {

    @Data
    public static class CreateInvoiceRequest {
        private Long companyId;
        private Long employeeId;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate billingPeriodFrom;
        private LocalDate billingPeriodTo;
        private String passengerName;
        private String mobile;
        private String pnr;
        private LocalDate dateOfTravel;
        private String fromCity;
        private String toCity;
        private BigDecimal cgstRate;
        private BigDecimal sgstRate;
        private List<LineItemRequest> lineItems;
    }

    @Data
    public static class LineItemRequest {
        private Long ticketId;
        private String particulars;
        private String sacCode;
        private BigDecimal taxableValue;
        private BigDecimal nonTaxableValue;
        private BigDecimal total;
        private Boolean isManuallyAdded = false;
    }

    @Data
    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate billingPeriodFrom;
        private LocalDate billingPeriodTo;
        // Passenger / ticket info
        private String passengerName;
        private String mobile;
        private String pnr;
        private LocalDate dateOfTravel;
        private String fromCity;
        private String toCity;
        // Company info
        private Long companyId;
        private String companyName;
        private String companyAddress;
        private String companyGstin;
        // Employee info
        private Long employeeId;
        private String employeeName;
        // Calculations
        private BigDecimal subtotal;
        private BigDecimal cgstRate;
        private BigDecimal sgstRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal grandTotal;
        private String totalInWords;
        private String status;
        private List<LineItemResponse> lineItems;
    }

    @Data
    public static class LineItemResponse {
        private Long id;
        private Long ticketId;
        private String particulars;
        private String sacCode;
        private BigDecimal taxableValue;
        private BigDecimal nonTaxableValue;
        private BigDecimal total;
        private Boolean isManuallyAdded;
    }

    @Data
    public static class TicketForBilling {
        private Long id;
        private String pnr;
        private LocalDate dateOfTravel;
        private String origin;
        private String destination;
        private String passengerName;
        private BigDecimal baseFare;
        private BigDecimal passengerServiceFee;
        private BigDecimal userDevelopmentCharges;
        private BigDecimal agentServiceCharges;
        private BigDecimal otherCharges;
        private BigDecimal discount;
        private String sacCodeAir;
        private String sacCodeAgent;
    }
}
