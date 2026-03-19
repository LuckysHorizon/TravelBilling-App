package com.travelbillpro.service;

import com.travelbillpro.dto.DashboardStatsDto;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        // 1. Current Month Revenue (Sum of totalAmount for APPROVED/BILLED/PAID tickets this month)
        BigDecimal currentMonthRevenue = ticketRepository.sumTotalAmountByStatusInAndDateBetween(
                java.util.List.of(TicketStatus.APPROVED, TicketStatus.BILLED, TicketStatus.PAID),
                startOfMonth,
                endOfMonth
        );
        if (currentMonthRevenue == null) currentMonthRevenue = BigDecimal.ZERO;

        // 2. Current Month Tickets Count
        Long currentMonthTickets = ticketRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59)
        );

        // 3. Pending Invoices (Draft or Sent)
        Long pendingInvoicesCount = invoiceRepository.countByStatusIn(
                java.util.List.of(InvoiceStatus.DRAFT, InvoiceStatus.SENT)
        );

        // 4. Pending Review Tickets
        Long pendingTicketsCount = ticketRepository.countByStatus(TicketStatus.PENDING_REVIEW);

        // 5. Outstanding Balance (Total of unpaid sent invoices)
        BigDecimal outstandingBalance = invoiceRepository.sumGrandTotalByStatus(InvoiceStatus.SENT);
        if (outstandingBalance == null) outstandingBalance = BigDecimal.ZERO;

        return new DashboardStatsDto(
                currentMonthRevenue,
                currentMonthTickets,
                pendingInvoicesCount,
                pendingTicketsCount,
                outstandingBalance
        );
    }
}
