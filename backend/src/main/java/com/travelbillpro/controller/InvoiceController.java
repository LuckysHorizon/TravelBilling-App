package com.travelbillpro.controller;

import com.travelbillpro.dto.InvoiceDto;
import com.travelbillpro.entity.Invoice;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.TicketRepository;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.EmailService;
import com.travelbillpro.service.InvoiceGeneratorService;
import com.travelbillpro.service.InvoiceService;
import com.travelbillpro.service.LocalFileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceGeneratorService invoiceGeneratorService;
    private final LocalFileStorageService fileStorageService;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<Page<InvoiceDto.InvoiceResponse>> getAllInvoices(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto.InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<InvoiceDto.InvoiceResponse>> getInvoicesByCompany(@PathVariable Long companyId, Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCompany(companyId, pageable));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<InvoiceDto.InvoiceResponse> generateInvoice(
            @Valid @RequestBody InvoiceDto.GenerateInvoiceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(invoiceService.generateInvoice(request, userDetails.getUser()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<InvoiceDto.InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDto.UpdateInvoiceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request, userDetails.getUser()));
    }
    
    @PostMapping("/{id}/mark-sent")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<Void> markAsSent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        invoiceService.markAsSent(id, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<Void> markAsPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        invoiceService.markAsPaid(id, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PDF DOWNLOAD
    // ═════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/download-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (invoice.getPdfFilePath() == null) {
            throw new BusinessException("PDF not yet generated", "NO_PDF", HttpStatus.NOT_FOUND);
        }

        Resource resource = fileStorageService.loadFileAsResource(invoice.getPdfFilePath());
        String safeFilename = invoice.getInvoiceNumber().replace("/", "-") + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                .body(resource);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  EXCEL / CSV DOWNLOAD
    // ═════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/download-excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<Ticket> tickets = ticketRepository.findByInvoiceId(id);
        byte[] excelBytes = invoiceGeneratorService.generateExcel(invoice, tickets);
        String safeFilename = invoice.getInvoiceNumber().replace("/", "-") + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                .body(excelBytes);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  EMAIL SEND
    // ═════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/send-email")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<Map<String, String>> sendEmail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));

        emailService.sendInvoiceEmail(invoice);
        invoiceService.markAsSent(id, userDetails.getUser());

        return ResponseEntity.ok(Map.of("message", "Invoice emailed to " + invoice.getCompany().getBillingEmail()));
    }
}
