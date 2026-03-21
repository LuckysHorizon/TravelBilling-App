package com.travelbillpro.service;

import com.travelbillpro.dto.DashboardStatsDto;
import com.travelbillpro.entity.Company;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        BigDecimal currentMonthRevenue = ticketRepository.sumTotalAmountByStatusInAndDateBetween(
                List.of(TicketStatus.APPROVED, TicketStatus.BILLED, TicketStatus.PAID),
                startOfMonth, endOfMonth
        );
        if (currentMonthRevenue == null) currentMonthRevenue = BigDecimal.ZERO;

        Long currentMonthTickets = ticketRepository.countByCreatedAtBetween(
                startOfMonth.atStartOfDay(), endOfMonth.atTime(23, 59, 59)
        );

        Long pendingInvoicesCount = invoiceRepository.countByStatusIn(
                List.of(InvoiceStatus.DRAFT, InvoiceStatus.SENT)
        );

        Long pendingTicketsCount = ticketRepository.countByStatus(TicketStatus.PENDING_REVIEW);

        BigDecimal outstandingBalance = invoiceRepository.sumGrandTotalByStatus(InvoiceStatus.SENT);
        if (outstandingBalance == null) outstandingBalance = BigDecimal.ZERO;

        return new DashboardStatsDto(
                currentMonthRevenue, currentMonthTickets,
                pendingInvoicesCount, pendingTicketsCount, outstandingBalance
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDate start = month.withDayOfMonth(1);
            LocalDate end = month.withDayOfMonth(month.lengthOfMonth());

            BigDecimal revenue = ticketRepository.sumTotalAmountByStatusInAndDateBetween(
                    List.of(TicketStatus.APPROVED, TicketStatus.BILLED, TicketStatus.PAID),
                    start, end
            );
            if (revenue == null) revenue = BigDecimal.ZERO;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            entry.put("revenue", revenue);
            entry.put("isCurrent", i == 0);
            result.add(entry);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getClientBreakdown() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Company> companies = companyRepository.findAll();

        for (Company company : companies) {
            BigDecimal totalSpend = ticketRepository.sumTotalAmountByStatusInAndDateBetween(
                    List.of(TicketStatus.APPROVED, TicketStatus.BILLED, TicketStatus.PAID),
                    LocalDate.of(LocalDate.now().getYear(), 1, 1),
                    LocalDate.now()
            );
            if (totalSpend == null) totalSpend = BigDecimal.ZERO;

            long ticketCount = ticketRepository.countByCreatedAtBetween(
                    LocalDate.of(LocalDate.now().getYear(), 1, 1).atStartOfDay(),
                    LocalDate.now().atTime(23, 59, 59)
            );

            BigDecimal outstanding = invoiceRepository.sumGrandTotalByStatus(InvoiceStatus.SENT);
            if (outstanding == null) outstanding = BigDecimal.ZERO;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", company.getId());
            entry.put("name", company.getName());
            entry.put("tickets", ticketCount);
            entry.put("spend", totalSpend);
            entry.put("outstanding", outstanding);
            result.add(entry);
        }
        return result;
    }
}
