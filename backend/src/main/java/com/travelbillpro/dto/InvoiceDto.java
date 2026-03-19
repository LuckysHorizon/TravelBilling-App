package com.travelbillpro.dto;

import com.travelbillpro.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvoiceDto {

    @Data
    public static class GenerateInvoiceRequest {
        @NotNull(message = "Company ID is required")
        private Long companyId;
        
        @NotNull(message = "Billing start date is required")
        private LocalDate startDate;
        
        @NotNull(message = "Billing end date is required")
        private LocalDate endDate;
    }

    @Data
    public static class InvoiceResponse {
        private Long id;
        private Long companyId;
        private String companyName;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private LocalDate billingPeriodStart;
        private LocalDate billingPeriodEnd;
        private BigDecimal totalBaseFare;
        private BigDecimal totalServiceCharge;
        private BigDecimal totalCgst;
        private BigDecimal totalSgst;
        private BigDecimal grandTotal;
        private InvoiceStatus status;
        private String filePath;
        private Integer ticketCount;
        private LocalDateTime createdAt;
        private Long createdById;
    }
}
