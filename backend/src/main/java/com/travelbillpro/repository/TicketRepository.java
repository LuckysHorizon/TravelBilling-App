package com.travelbillpro.repository;

import com.travelbillpro.entity.Ticket;
import com.travelbillpro.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByPnrNumber(String pnrNumber);
    boolean existsByPnrNumber(String pnrNumber);
    
    Page<Ticket> findByCompanyId(Long companyId, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    
    List<Ticket> findByCompanyIdAndStatusAndTravelDateBetween(
            Long companyId, 
            TicketStatus status, 
            LocalDate startDate, 
            LocalDate endDate
    );
}
