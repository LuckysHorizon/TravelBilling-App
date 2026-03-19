package com.travelbillpro.dto;

import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TicketDto {

    @Data
    public static class TicketResponse {
        private Long id;
        private Long companyId;
        private String companyName;
        private String pnrNumber;
        private TicketType ticketType;
        private String passengerName;
        private LocalDate travelDate;
        private String origin;
        private String destination;
        private String operatorName;
        private BigDecimal baseFare;
        private BigDecimal serviceCharge;
        private BigDecimal cgst;
        private BigDecimal sgst;
        private BigDecimal totalAmount;
        private TicketStatus status;
        private String filePath;
        private BigDecimal aiConfidence;
        private Long invoiceId;
        private LocalDateTime createdAt;
        private Long createdById;
    }

    @Data
    public static class UpdateTicketRequest {
        @NotBlank(message = "PNR is required")
        private String pnrNumber;
        
        @NotNull(message = "Ticket type is required")
        private TicketType ticketType;
        
        @NotBlank(message = "Passenger name is required")
        private String passengerName;
        
        @NotNull(message = "Travel date is required")
        private LocalDate travelDate;
        
        private String origin;
        private String destination;
        private String operatorName;
        
        @NotNull(message = "Base fare is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Base fare must be greater than 0")
        private BigDecimal baseFare;
    }
}
