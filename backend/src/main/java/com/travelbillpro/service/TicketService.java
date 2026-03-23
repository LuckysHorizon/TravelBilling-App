package com.travelbillpro.service;

import com.travelbillpro.client.PythonExtractionClient;
import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.dto.TicketExtractionResult;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.ExtractionAudit;
import com.travelbillpro.entity.GstConfig;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.exception.ExtractionException;
import com.travelbillpro.exception.NvidiaApiException;
import com.travelbillpro.exception.PdfExtractionException;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.ExtractionAuditRepository;
import com.travelbillpro.repository.GstConfigRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final GstConfigRepository gstConfigRepository;
    private final ExtractionAuditRepository extractionAuditRepository;
    private final LocalFileStorageService fileStorageService;
    private final TicketParserService ticketParserService;
    private final NvidiaExtractionService nvidiaExtractionService;
    private final AuditService auditService;
    private final PythonExtractionClient pythonExtractionClient;
    
    // Unified extraction pipeline services
    private final TicketPersistenceService persistenceService;
    private final ExtractionAuditService extractionAuditService;
    private final TicketValidationService validationService;

    // ═════════════════════════════════════════════════════════════════════════
    //  READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<TicketDto.TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketDto.TicketResponse> searchTickets(String search, Pageable pageable) {
        return ticketRepository.searchByPnrOrPassenger(search, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketDto.TicketResponse> getTicketsByCompany(Long companyId, Pageable pageable) {
        return ticketRepository.findByCompanyId(companyId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TicketDto.TicketResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Ticket getTicketEntity(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ticket not found", "TICKET_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public TicketDto.TicketResponse getTicketById(Long id) {
        return mapToResponse(getTicketEntity(id));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UPLOAD ORCHESTRATION — main entry point
    //  
    //  CRITICAL: This method has NO @Transactional annotation.
    //  - PDF parsing, AI calls, and validation happen here (outside any transaction)
    //  - Only final DB writes open clean transactions
    //  - Audit writes happen in their own REQUIRES_NEW transactions
    // ═════════════════════════════════════════════════════════════════════════
    //  UPLOAD ORCHESTRATION — UNIFIED PIPELINE
    //  
    //  One AI call per PDF, handles all structures (single, group, multi-leg).
    //  The SYSTEM_PROMPT teaches the model to handle everything.
    // ═════════════════════════════════════════════════════════════════════════

    public List<TicketDto.TicketResponse> processTicketUploads(Long companyId, TicketType expectedType,
                                                                List<MultipartFile> files, User user) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<TicketDto.TicketResponse> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            long startMs = System.currentTimeMillis();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf";

            AuditContext auditContext = new AuditContext(filename);

            try {
                // ──── STEP 1: Store file locally ─────────────────────────────────
                String storedPath = fileStorageService.storeTicketContent(companyId, file);
                log.info("=== Pipeline START: {} ===", filename);

                // ──── STEP 2: Resolve absolute path for Python service ───────────
                String absolutePath = fileStorageService.resolveAbsolutePath(storedPath);
                log.info("Resolved absolute path: {}", absolutePath);

                // ──── STEP 3: Call Python extraction service ─────────────────────
                //  Python renders PDF pages via PyMuPDF → sends to NVIDIA vision
                //  Returns structured JSON with passenger records
                @SuppressWarnings("unchecked")
                Map<String, Object> extractionResponse = 
                        pythonExtractionClient.extract(absolutePath, companyId);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = 
                        (List<Map<String, Object>>) extractionResponse.get("records");

                String extractionStatus = (String) extractionResponse.get("status");
                log.info("Python extraction returned status={}, records={}", 
                        extractionStatus, records != null ? records.size() : 0);

                // Record audit info from Python response
                auditContext.setModelUsed(str(extractionResponse, "model_used"));

                if (records == null || records.isEmpty()) {
                    throw new ExtractionException(
                        "Python extraction returned no records for: " + filename);
                }

                // ──── STEP 4: Map each record to Ticket entity ──────────────────
                List<Ticket> tickets = records.stream()
                    .map(record -> mapAiRecordToTicket(record, storedPath, company, expectedType, user))
                    .toList();

                if (tickets.isEmpty()) {
                    throw new ExtractionException("No tickets extracted from AI response for: " + filename);
                }
                log.info("Mapped {} ticket(s) from Python response", tickets.size());

                // ──── STEP 5: Persist all tickets ────────────────────────────────
                List<Ticket> saved = persistenceService.saveAll(tickets);
                log.info("Persisted {} ticket(s)", saved.size());

                for (Ticket t : saved) {
                    try {
                        auditService.logAction("TICKET", t.getId(), "UPLOADED", null, mapToResponse(t), user);
                    } catch (Exception auditEx) {
                        log.warn("Audit log failed for ticket {}: {}", t.getId(), auditEx.getMessage());
                    }
                    results.add(mapToResponse(t));
                }

                // ──── STEP 6: Write audit ────────────────────────────────────────
                auditContext.setStatus("SUCCESS");
                auditContext.setProcessingMs(System.currentTimeMillis() - startMs);
                extractionAuditService.writeAudit(auditContext);
                log.info("Pipeline complete: {} → {} ticket(s) in {}ms",
                        filename, saved.size(), System.currentTimeMillis() - startMs);

            } catch (ExtractionException | PdfExtractionException | NvidiaApiException e) {
                log.warn("Extraction failed for {}: {}", filename, e.getMessage());
                errors.add(filename + ": " + e.getMessage());
                writeFailedAudit(auditContext, e, startMs);

            } catch (Exception e) {
                log.error("Unexpected pipeline error for {}: {}", filename, e.getMessage(), e);
                errors.add(filename + ": " + e.getMessage());
                writeFailedAudit(auditContext, e, startMs);
            }
        }

        if (results.isEmpty()) {
            String errorDetail = errors.isEmpty() 
                ? "Unknown error during processing"
                : String.join("; ", errors);
            throw new ExtractionException(
                "Upload completed but ticket persistence failed: " + errorDetail);
        }

        return results;
    }

    /**
     * Map AI record → Ticket entity.
     * AI returns: pnr_number, passenger_name, travel_date, operator_name,
     *             origin, destination, base_fare, total_amount, ticket_type, confidence
     */
    private Ticket mapAiRecordToTicket(Map<String, Object> record,
                                        String filePath,
                                        Company company,
                                        TicketType expectedType,
                                        User user) {
        Ticket ticket = new Ticket();

        // PNR — trim to 20 chars (DB constraint)
        String pnr = str(record, "pnr_number");
        ticket.setPnrNumber(pnr != null ? cap(pnr.toUpperCase().trim(), 20) : null);

        ticket.setPassengerName(str(record, "passenger_name"));
        ticket.setOrigin(str(record, "origin"));
        ticket.setDestination(str(record, "destination"));
        ticket.setOperatorName(str(record, "operator_name"));

        // Ticket type
        String typeStr = str(record, "ticket_type");
        TicketType resolvedType = expectedType;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                resolvedType = TicketType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                resolvedType = expectedType != null ? expectedType : TicketType.UNKNOWN;
            }
        }
        ticket.setTicketType(resolvedType != null ? resolvedType : TicketType.UNKNOWN);

        // Travel date — parse yyyy-MM-dd
        String dateStr = str(record, "travel_date");
        if (dateStr != null) {
            try {
                ticket.setTravelDate(java.time.LocalDate.parse(dateStr));
            } catch (Exception e) {
                log.warn("Could not parse travel_date '{}' — setting null", dateStr);
                ticket.setTravelDate(null);
            }
        }

        // Fares — no service tax, grand total = base fare
        BigDecimal baseFare = decimal(record, "base_fare");
        ticket.setBaseFare(baseFare);
        ticket.setTotalAmount(baseFare); // total = base (no tax)
        ticket.setAiConfidence(decimal(record, "confidence"));

        // No service tax
        ticket.setServiceCharge(BigDecimal.ZERO);
        ticket.setCgst(BigDecimal.ZERO);
        ticket.setSgst(BigDecimal.ZERO);

        // System fields
        ticket.setFilePath(filePath);
        ticket.setCompany(company);
        ticket.setCreatedById(user.getId());
        ticket.setStatus(TicketStatus.PENDING_REVIEW);

        return ticket;
    }

    /**
     * Audit writer — called in catch blocks, must never throw.
     * Delegates to ExtractionAuditService which has REQUIRES_NEW propagation.
     */
    private void writeFailedAudit(AuditContext ctx, Exception e, long startMs) {
        try {
            ctx.setStatus("FAILED");
            ctx.setErrorMessage(e.getMessage());
            ctx.setProcessingMs(System.currentTimeMillis() - startMs);
            extractionAuditService.writeAudit(ctx);
        } catch (Exception auditEx) {
            // Audit failure must never mask the original exception
            log.error("Failed to write audit record: {}", auditEx.getMessage());
        }
    }

    /**
     * Validate all tickets — returns warnings, never throws.
     */
    private List<String> validateAll(List<Ticket> tickets) {
        List<String> warnings = new ArrayList<>();
        for (Ticket ticket : tickets) {
            TicketValidationService.ValidationReport report = validationService.validate(ticket);
            if (!report.errors().isEmpty()) {
                log.warn("Ticket {} has validation errors: {}", ticket.getPnrNumber(), report.errors());
                warnings.addAll(report.errors());
            }
            if (!report.warnings().isEmpty()) {
                warnings.addAll(report.warnings());
            }
        }
        return warnings;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UNIFIED EXTRACTION CONSOLIDATION
    //  Removed old methods: processGroupBooking, processSingleBooking,
    //  parseSingleResult, buildTicketFromExtraction, resolvePerPassengerFare,
    //  safePnr, ensureUniquePnr, safeDate, validateAll
    //  (replaced by simplified one-call pipeline in processTicketUploads with mapAiRecordToTicket)
    // ═════════════════════════════════════════════════════════════════════════

    // ═════════════════════════════════════════════════════════════════════════
    //  UPDATE & APPROVE
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public TicketDto.TicketResponse updateAndApproveTicket(Long id, TicketDto.UpdateTicketRequest request, User user) {
        Ticket ticket = getTicketEntity(id);

        if (ticket.getStatus() == TicketStatus.BILLED || ticket.getStatus() == TicketStatus.PAID) {
            throw new BusinessException("Cannot modify a billed ticket", "TICKET_LOCKED", HttpStatus.FORBIDDEN);
        }

        // Check PNR uniqueness if changed
        if (!ticket.getPnrNumber().equals(request.getPnrNumber()) &&
                ticketRepository.existsByPnrNumber(request.getPnrNumber())) {
            throw new BusinessException("PNR already exists", "DUPLICATE_PNR", HttpStatus.CONFLICT);
        }

        TicketDto.TicketResponse oldValue = mapToResponse(ticket);

        ticket.setPnrNumber(request.getPnrNumber());
        ticket.setTicketType(request.getTicketType());
        ticket.setPassengerName(request.getPassengerName());
        ticket.setTravelDate(request.getTravelDate());
        ticket.setOrigin(request.getOrigin());
        ticket.setDestination(request.getDestination());
        ticket.setOperatorName(request.getOperatorName());
        ticket.setBaseFare(request.getBaseFare());

        // No service tax — grand total = base fare
        ticket.setServiceCharge(BigDecimal.ZERO);
        ticket.setCgst(BigDecimal.ZERO);
        ticket.setSgst(BigDecimal.ZERO);
        ticket.setTotalAmount(request.getBaseFare());

        ticket.setStatus(TicketStatus.APPROVED);

        Ticket savedTicket = ticketRepository.save(ticket);
        TicketDto.TicketResponse newValue = mapToResponse(savedTicket);

        auditService.logAction("TICKET", savedTicket.getId(), "APPROVED", oldValue, newValue, user);

        return newValue;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteTicket(Long id, User user) {
        Ticket ticket = getTicketEntity(id);

        if (ticket.getStatus() == TicketStatus.BILLED || ticket.getStatus() == TicketStatus.PAID) {
            throw new BusinessException("Cannot delete a billed ticket", "TICKET_LOCKED", HttpStatus.FORBIDDEN);
        }

        auditService.logAction("TICKET", ticket.getId(), "DELETED", mapToResponse(ticket), null, user);

        if (ticket.getFilePath() != null) {
            fileStorageService.deleteFile(ticket.getFilePath());
        }

        ticketRepository.delete(ticket);
    }

    @Transactional
    public int deleteTicketsBatch(List<Long> ids, User user) {
        int deleted = 0;
        for (Long id : ids) {
            try {
                deleteTicket(id, user);
                deleted++;
            } catch (BusinessException e) {
                log.warn("Skipped deletion of ticket {}: {}", id, e.getMessage());
            }
        }
        return deleted;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private String coalesce(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private String cap(String s, int max) {
        return s == null ? null : s.length() <= max ? s : s.substring(0, max);
    }

    private BigDecimal decimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try {
            return new BigDecimal(v.toString().replace(",", ""))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MAPPER
    // ═════════════════════════════════════════════════════════════════════════

    private TicketDto.TicketResponse mapToResponse(Ticket ticket) {
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
