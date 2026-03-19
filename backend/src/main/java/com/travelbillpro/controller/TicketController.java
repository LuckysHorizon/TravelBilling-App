package com.travelbillpro.controller;

import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<Page<TicketDto.TicketResponse>> getAllTickets(Pageable pageable) {
        return ResponseEntity.ok(ticketService.getAllTickets(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto.TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<TicketDto.TicketResponse>> getTicketsByCompany(
            @PathVariable Long companyId, Pageable pageable) {
        return ResponseEntity.ok(ticketService.getTicketsByCompany(companyId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TicketDto.TicketResponse>> getTicketsByStatus(
            @PathVariable TicketStatus status, Pageable pageable) {
        return ResponseEntity.ok(ticketService.getTicketsByStatus(status, pageable));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<List<TicketDto.TicketResponse>> uploadTickets(
            @RequestParam("companyId") Long companyId,
            @RequestParam("ticketType") TicketType expectedType,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(
                ticketService.processTicketUploads(companyId, expectedType, files, userDetails.getUser()),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<TicketDto.TicketResponse> updateAndApproveTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketDto.UpdateTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ticketService.updateAndApproveTicket(id, request, userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ticketService.deleteTicket(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
