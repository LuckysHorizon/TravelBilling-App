package com.travelbillpro.service;

import com.travelbillpro.dto.InvoiceDto;
import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.Invoice;
import com.travelbillpro.entity.InvoiceSequence;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.InvoiceSequenceRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final InvoiceSequenceRepository sequenceRepository;
    private final AuditService auditService;
    private final LocalFileStorageService fileStorageService;
    private final InvoiceGeneratorService invoiceGeneratorService;

    @Transactional(readOnly = true)
    public Page<InvoiceDto.InvoiceResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<InvoiceDto.InvoiceResponse> getInvoicesByCompany(Long companyId, Pageable pageable) {
        return invoiceRepository.findByCompanyId(companyId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceDto.InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToResponse(invoice);
    }

    /**
     * Generates an invoice for a company for a specific billing period.
     * Uses pessimistic locking on the sequence table to ensure gapless invoice numbers.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InvoiceDto.InvoiceResponse generateInvoice(InvoiceDto.GenerateInvoiceRequest request, User user) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Find all APPROVED tickets for this company in the date range
        List<Ticket> unbilledTickets = ticketRepository.findUnbilledTicketsForCompanyAndPeriod(
                company.getId(), request.getStartDate(), request.getEndDate()
        );

        if (unbilledTickets.isEmpty()) {
            throw new BusinessException("No approved unbilled tickets found for this period", "NO_TICKETS", HttpStatus.BAD_REQUEST);
        }

        // Calculate totals
        BigDecimal totalBaseFare = BigDecimal.ZERO;
        BigDecimal totalServiceCharge = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Ticket ticket : unbilledTickets) {
            totalBaseFare = totalBaseFare.add(ticket.getBaseFare() != null ? ticket.getBaseFare() : BigDecimal.ZERO);
            totalServiceCharge = totalServiceCharge.add(ticket.getServiceCharge() != null ? ticket.getServiceCharge() : BigDecimal.ZERO);
            totalCgst = totalCgst.add(ticket.getCgst() != null ? ticket.getCgst() : BigDecimal.ZERO);
            totalSgst = totalSgst.add(ticket.getSgst() != null ? ticket.getSgst() : BigDecimal.ZERO);
            grandTotal = grandTotal.add(ticket.getTotalAmount() != null ? ticket.getTotalAmount() : BigDecimal.ZERO);
        }

        // Generate sequential invoice number
        String financialYear = getFinancialYear(LocalDate.now());
        String invoiceNumber = generateNextInvoiceNumber(financialYear);

        // Create Invoice
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(company.getBillingCycle() == com.travelbillpro.enums.BillingCycle.MONTHLY ? 15 : 7)); // Simple due date logic
        invoice.setBillingPeriodStart(request.getStartDate());
        invoice.setBillingPeriodEnd(request.getEndDate());
        invoice.setBillingMonth(request.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        invoice.setSubtotal(totalBaseFare);
        invoice.setServiceCharge(totalServiceCharge);
        invoice.setCgstTotal(totalCgst);
        invoice.setSgstTotal(totalSgst);
        invoice.setGrandTotal(grandTotal);
        invoice.setStatus(InvoiceStatus.DRAFT); // DRAFT until file is generated
        invoice.setCreatedById(user.getId());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Link tickets to invoice and update status
        for (Ticket ticket : unbilledTickets) {
            ticket.setInvoice(savedInvoice);
            ticket.setStatus(TicketStatus.BILLED);
            ticketRepository.save(ticket); // Note: we could batch update for performance if volumes are huge
        }
        
        auditService.logAction("INVOICE", savedInvoice.getId(), "GENERATED", null, mapToResponse(savedInvoice), user);
        
        // Generate physical physical PDF
        byte[] pdfBytes = invoiceGeneratorService.generatePdf(savedInvoice, unbilledTickets);
        String filePath = fileStorageService.storeInvoiceFile(company.getId(), invoiceNumber, pdfBytes, "pdf");
        savedInvoice.setPdfFilePath(filePath);
        savedInvoice.setStatus(InvoiceStatus.GENERATED);
        invoiceRepository.save(savedInvoice);

        return mapToResponse(savedInvoice);
    }

    /**
     * Update invoice totals (service charge, CGST, SGST, grand total).
     * Only allowed on DRAFT or GENERATED invoices.
     * Regenerates PDF after update.
     */
    @Transactional
    public InvoiceDto.InvoiceResponse updateInvoice(Long id, InvoiceDto.UpdateInvoiceRequest request, User user) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (invoice.getStatus() != InvoiceStatus.DRAFT && invoice.getStatus() != InvoiceStatus.GENERATED) {
            throw new BusinessException("Cannot edit a sent or paid invoice", "INVOICE_LOCKED", HttpStatus.FORBIDDEN);
        }

        InvoiceDto.InvoiceResponse oldValue = mapToResponse(invoice);

        if (request.getServiceCharge() != null) invoice.setServiceCharge(request.getServiceCharge());
        if (request.getCgstTotal() != null) invoice.setCgstTotal(request.getCgstTotal());
        if (request.getSgstTotal() != null) invoice.setSgstTotal(request.getSgstTotal());
        if (request.getGrandTotal() != null) invoice.setGrandTotal(request.getGrandTotal());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Regenerate PDF with updated values
        try {
            List<Ticket> tickets = ticketRepository.findTicketsByInvoiceId(savedInvoice.getId());
            byte[] pdfBytes = invoiceGeneratorService.generatePdf(savedInvoice, tickets);
            String filePath = fileStorageService.storeInvoiceFile(
                    savedInvoice.getCompany().getId(),
                    savedInvoice.getInvoiceNumber(),
                    pdfBytes, "pdf");
            savedInvoice.setPdfFilePath(filePath);
            savedInvoice.setStatus(InvoiceStatus.GENERATED);
            invoiceRepository.save(savedInvoice);
        } catch (Exception e) {
            log.warn("Failed to regenerate PDF after invoice edit: {}", e.getMessage());
        }

        auditService.logAction("INVOICE", savedInvoice.getId(), "UPDATED", oldValue, mapToResponse(savedInvoice), user);
        return mapToResponse(savedInvoice);
    }
    
    @Transactional
    public void markAsSent(Long id, User user) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
                
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Invoice is already paid", "INVALID_STATE", HttpStatus.BAD_REQUEST);
        }
        
        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.SENT);
        invoiceRepository.save(invoice);
        
        auditService.logAction("INVOICE", invoice.getId(), "MARKED_SENT", oldStatus.name(), invoice.getStatus().name(), user);
    }
    
    @Transactional
    public void markAsPaid(Long id, User user) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
                
        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
        
        // Update associated tickets
        List<Ticket> tickets = ticketRepository.findTicketsByInvoiceId(invoice.getId());
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.PAID);
            ticketRepository.save(ticket);
        }
        
        auditService.logAction("INVOICE", invoice.getId(), "MARKED_PAID", oldStatus.name(), invoice.getStatus().name(), user);
    }

    /**
     * Obtains a pessimistic write lock on the sequence record to ensure no two requests
     * can claim the same invoice number for a given financial year.
     */
    private String generateNextInvoiceNumber(String financialYear) {
        InvoiceSequence sequence = sequenceRepository.findByFinancialYear(financialYear)
                .orElseGet(() -> {
                    // Create if it doesn't exist
                    InvoiceSequence newSeq = new InvoiceSequence();
                    newSeq.setFinancialYear(financialYear);
                    newSeq.setNextValue(1L);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        // The query in the repository uses @Lock(PessimisticLocking.WRITE)
        // Wait, the findByFinancialYear above didn't use the lock method. Let's fetch it with lock.
        sequence = sequenceRepository.findByFinancialYearWithLock(financialYear)
                .orElseThrow(() -> new IllegalStateException("Failed to acquire sequence lock"));

        Long currentVal = sequence.getNextValue();
        sequence.setNextValue(currentVal + 1);
        sequenceRepository.save(sequence);

        return String.format("TBP/%s/%05d", financialYear, currentVal);
    }

    private String getFinancialYear(LocalDate date) {
        int year = date.getYear();
        // Indian Financial Year string: "YYYY-YY" (e.g. 2023-24)
        if (date.getMonthValue() < 4) {
            return (year - 1) + "-" + String.format("%02d", year % 100);
        } else {
            return year + "-" + String.format("%02d", (year + 1) % 100);
        }
    }

    private InvoiceDto.InvoiceResponse mapToResponse(Invoice invoice) {
        InvoiceDto.InvoiceResponse response = new InvoiceDto.InvoiceResponse();
        response.setId(invoice.getId());
        response.setCompanyId(invoice.getCompany().getId());
        response.setCompanyName(invoice.getCompany().getName());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDueDate(invoice.getDueDate());
        response.setBillingPeriodStart(invoice.getBillingPeriodStart());
        response.setBillingPeriodEnd(invoice.getBillingPeriodEnd());
        response.setSubtotal(invoice.getSubtotal());
        response.setServiceCharge(invoice.getServiceCharge());
        response.setCgstTotal(invoice.getCgstTotal());
        response.setSgstTotal(invoice.getSgstTotal());
        response.setGrandTotal(invoice.getGrandTotal());
        response.setStatus(invoice.getStatus());
        response.setPdfFilePath(invoice.getPdfFilePath());
        
        if (invoice.getTickets() != null) {
            response.setTickets(invoice.getTickets().stream()
                .map(this::mapToTicketResponse)
                .collect(java.util.stream.Collectors.toList()));
        }
        response.setCreatedAt(invoice.getCreatedAt());
        response.setCreatedById(invoice.getCreatedById());
        
        // Count tickets if lazy loaded (careful with N+1 queries in a real high-scale app)
        response.setTicketCount(invoice.getTickets() != null ? invoice.getTickets().size() : 0);
        
        return response;
    }
    private TicketDto.TicketResponse mapToTicketResponse(Ticket ticket) {
        TicketDto.TicketResponse response = new TicketDto.TicketResponse();
        response.setId(ticket.getId());
        response.setCompanyId(ticket.getCompany().getId());
        response.setCompanyName(ticket.getCompany().getName());
        response.setPnrNumber(ticket.getPnrNumber());
        response.setTicketType(ticket.getTicketType());
        response.setPassengerName(ticket.getPassengerName());
        response.setTravelDate(ticket.getTravelDate());
        response.setOrigin(ticket.getOrigin());
        response.setDestination(ticket.getDestination());
        response.setOperatorName(ticket.getOperatorName());
        response.setBaseFare(ticket.getBaseFare());
        response.setServiceCharge(ticket.getServiceCharge());
        response.setCgst(ticket.getCgst());
        response.setSgst(ticket.getSgst());
        response.setTotalAmount(ticket.getTotalAmount());
        response.setStatus(ticket.getStatus());
        response.setFilePath(ticket.getFilePath());
        response.setAiConfidence(ticket.getAiConfidence());
        response.setInvoiceId(ticket.getInvoice() != null ? ticket.getInvoice().getId() : null);
        response.setCreatedAt(ticket.getCreatedAt());
        response.setCreatedById(ticket.getCreatedById());
        return response;
    }
}
