package com.travelbillpro.repository;

import com.travelbillpro.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByCompanyIdAndBillingMonth(Long companyId, String billingMonth);
    Page<Invoice> findByCompanyId(Long companyId, Pageable pageable);
}
