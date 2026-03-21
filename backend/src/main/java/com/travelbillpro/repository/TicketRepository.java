package com.travelbillpro.repository;

import com.travelbillpro.entity.Ticket;
import com.travelbillpro.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByPnrNumber(String pnrNumber);
    boolean existsByPnrNumber(String pnrNumber);
    
    Page<Ticket> findByCompanyId(Long companyId, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    long countByStatus(TicketStatus status);
    
    @Query("SELECT SUM(t.totalAmount) FROM Ticket t WHERE t.status IN :statuses AND t.travelDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStatusInAndDateBetween(List<TicketStatus> statuses, LocalDate startDate, LocalDate endDate);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Ticket> findByCompanyIdAndStatusAndTravelDateBetween(
            Long companyId, 
            TicketStatus status, 
            LocalDate startDate, 
            LocalDate endDate
    );

    default List<Ticket> findUnbilledTicketsForCompanyAndPeriod(Long companyId, LocalDate startDate, LocalDate endDate) {
        return findByCompanyIdAndStatusAndTravelDateBetween(companyId, TicketStatus.APPROVED, startDate, endDate);
    }

    List<Ticket> findByInvoiceId(Long invoiceId);

    List<Ticket> findByBillingPanelId(Long billingPanelId);

    default List<Ticket> findTicketsByInvoiceId(Long invoiceId) {
        return findByInvoiceId(invoiceId);
    }
}
