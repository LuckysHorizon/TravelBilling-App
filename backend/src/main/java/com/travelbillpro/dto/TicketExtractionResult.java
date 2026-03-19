package com.travelbillpro.dto;

import com.travelbillpro.enums.TicketType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TicketExtractionResult {
    private String pnrNumber;
    private BigDecimal pnrConfidence;
    
    private String passengerName;
    private BigDecimal passengerConfidence;
    
    private LocalDate travelDate;
    private BigDecimal dateConfidence;
    
    private BigDecimal baseFare;
    private BigDecimal amountConfidence;
    
    private String origin;
    private String destination;
    private String operatorName;
    
    private TicketType ticketType;
    
    private BigDecimal overallConfidence;
    private String extractionMethod; // "AI" or "REGEX"
}
