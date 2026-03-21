package com.travelbillpro.controller;

import com.travelbillpro.dto.BillingPanelDto;
import com.travelbillpro.dto.InvoiceDto;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.BillingPanelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing-panels")
@RequiredArgsConstructor
public class BillingPanelController {

    private final BillingPanelService billingPanelService;

    @GetMapping
    public ResponseEntity<List<BillingPanelDto.PanelResponse>> getAllPanels() {
        return ResponseEntity.ok(billingPanelService.getAllPanels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingPanelDto.PanelResponse> getPanelById(@PathVariable Long id) {
        return ResponseEntity.ok(billingPanelService.getPanelById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<BillingPanelDto.PanelResponse>> getPanelsByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(billingPanelService.getPanelsByCompany(companyId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<BillingPanelDto.PanelResponse> createPanel(
            @Valid @RequestBody BillingPanelDto.CreatePanelRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(billingPanelService.createPanel(request, userDetails.getUser()), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<BillingPanelDto.PanelResponse> addTickets(
            @PathVariable Long id,
            @Valid @RequestBody BillingPanelDto.AddTicketsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(billingPanelService.addTickets(id, request.getTicketIds(), userDetails.getUser()));
    }

    @DeleteMapping("/{id}/tickets/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<BillingPanelDto.PanelResponse> removeTicket(
            @PathVariable Long id, @PathVariable Long ticketId) {
        return ResponseEntity.ok(billingPanelService.removeTicket(id, ticketId));
    }

    @PostMapping("/{id}/generate-invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<InvoiceDto.InvoiceResponse> generateInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(billingPanelService.generateInvoiceFromPanel(id, userDetails.getUser()), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<Void> deletePanel(@PathVariable Long id) {
        billingPanelService.deletePanel(id);
        return ResponseEntity.noContent().build();
    }
}
