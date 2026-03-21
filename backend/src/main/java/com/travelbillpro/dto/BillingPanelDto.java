package com.travelbillpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BillingPanelDto {

    @Data
    public static class CreatePanelRequest {
        @NotBlank(message = "Panel label is required")
        private String label;

        @NotNull(message = "Company ID is required")
        private Long companyId;
    }

    @Data
    public static class AddTicketsRequest {
        @NotNull(message = "Ticket IDs are required")
        private List<Long> ticketIds;
    }

    @Data
    public static class PanelResponse {
        private Long id;
        private String label;
        private Long companyId;
        private String companyName;
        private String status;
        private Long invoiceId;
        private String invoiceNumber;
        private int ticketCount;
        private BigDecimal totalAmount;
        private List<TicketDto.TicketResponse> tickets;
        private LocalDateTime createdAt;
    }
}
