package com.travelbillpro.controller;

import com.travelbillpro.dto.InvoiceDto;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

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
}
