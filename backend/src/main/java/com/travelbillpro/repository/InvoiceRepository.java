package com.travelbillpro.repository;

import com.travelbillpro.entity.Invoice;
import com.travelbillpro.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByCompanyIdAndBillingMonth(Long companyId, String billingMonth);
    Page<Invoice> findByCompanyId(Long companyId, Pageable pageable);

    long countByStatusIn(List<InvoiceStatus> statuses);

    @Query("SELECT SUM(i.grandTotal) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumGrandTotalByStatus(InvoiceStatus status);

    @Query("SELECT SUM(i.grandTotal) FROM Invoice i WHERE i.company.id = :companyId AND i.status IN :statuses")
    BigDecimal sumGrandTotalByCompanyIdAndStatusIn(Long companyId, List<InvoiceStatus> statuses);

    @Query("SELECT SUM(i.grandTotal) FROM Invoice i WHERE i.company.id = :companyId AND i.status = :status")
    BigDecimal sumGrandTotalByCompanyIdAndStatus(Long companyId, InvoiceStatus status);
}
