package com.travelbillpro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private BigDecimal currentMonthRevenue;
    private Long currentMonthTickets;
    private Long pendingInvoicesCount;
    private Long pendingTicketsCount;
    private BigDecimal outstandingBalance;
}
