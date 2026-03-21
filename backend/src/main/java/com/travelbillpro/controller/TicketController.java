package com.travelbillpro.controller;

import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.LocalFileStorageService;
import com.travelbillpro.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
    private final LocalFileStorageService fileStorageService;

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
            @RequestParam(value = "ticketType", required = false, defaultValue = "UNKNOWN") TicketType expectedType,
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

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<java.util.Map<String, Object>> batchDelete(
            @RequestBody java.util.Map<String, java.util.List<Long>> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        java.util.List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "No ticket IDs provided"));
        }
        int deleted = ticketService.deleteTicketsBatch(ids, userDetails.getUser());
        return ResponseEntity.ok(java.util.Map.of("deleted", deleted, "total", ids.size()));
    }

    /**
     * Serves the PDF file associated with a ticket.
     * Uses ticket ID instead of file path to avoid absolute path encoding issues in URLs.
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getTicketFile(@PathVariable Long id) {
        TicketDto.TicketResponse ticket = ticketService.getTicketById(id);
        if (ticket.getFilePath() == null || ticket.getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = fileStorageService.loadFileAsResource(ticket.getFilePath());
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
