package com.travelbillpro.service;

import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.dto.TicketExtractionResult;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.GstConfig;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.CompanyRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final GstConfigRepository gstConfigRepository;
    private final LocalFileStorageService fileStorageService;
    private final TicketParserService ticketParserService;
    private final AiExtractionService aiExtractionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<TicketDto.TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::mapToResponse);
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

    @Transactional
    public List<TicketDto.TicketResponse> processTicketUploads(Long companyId, TicketType expectedType, List<MultipartFile> files, User user) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<TicketDto.TicketResponse> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 1. Store file locally
                String relativePath = fileStorageService.storeTicketContent(companyId, file);
                
                // 2. Extract text (OCR/PDFBox)
                String rawText = ticketParserService.extractTextFromFile(relativePath);
                
                // 3. Extract structured data via AI
                TicketExtractionResult extraction = aiExtractionService.extractTicketData(rawText, expectedType);
                
                // 4. Create pending ticket
                Ticket ticket = new Ticket();
                ticket.setCompany(company);
                ticket.setFilePath(relativePath);
                
                // Use a temporary PNR if extraction failed, user will fix it in review
                if (extraction.getPnrNumber() == null || extraction.getPnrNumber().trim().isEmpty()) {
                    ticket.setPnrNumber("PENDING_" + System.currentTimeMillis());
                } else {
                    // Check if PNR already exists
                    if (ticketRepository.existsByPnrNumber(extraction.getPnrNumber())) {
                        log.warn("Duplicate PNR detected during upload: {}", extraction.getPnrNumber());
                        ticket.setPnrNumber(extraction.getPnrNumber() + "_DUP_" + System.currentTimeMillis());
                    } else {
                        ticket.setPnrNumber(extraction.getPnrNumber());
                    }
                }
                
                ticket.setTicketType(expectedType);
                ticket.setPassengerName(extraction.getPassengerName() != null ? extraction.getPassengerName() : "Unknown");
                ticket.setTravelDate(extraction.getTravelDate() != null ? extraction.getTravelDate() : LocalDate.now());
                ticket.setOrigin(extraction.getOrigin());
                ticket.setDestination(extraction.getDestination());
                ticket.setOperatorName(extraction.getOperatorName());
                ticket.setBaseFare(extraction.getBaseFare() != null ? extraction.getBaseFare() : BigDecimal.ZERO);
                ticket.setAiConfidence(extraction.getOverallConfidence());
                
                // Set financials to zero initially (calculated upon approval)
                ticket.setServiceCharge(BigDecimal.ZERO);
                ticket.setCgst(BigDecimal.ZERO);
                ticket.setSgst(BigDecimal.ZERO);
                ticket.setTotalAmount(BigDecimal.ZERO);
                
                ticket.setStatus(TicketStatus.PENDING_REVIEW);
                ticket.setCreatedBy(user);
                
                Ticket savedTicket = ticketRepository.save(ticket);
                auditService.logAction("TICKET", savedTicket.getId(), "UPLOADED", null, mapToResponse(savedTicket), user);
                
                results.add(mapToResponse(savedTicket));
                
            } catch (Exception e) {
                log.error("Error processing ticket file: {}", file.getOriginalFilename(), e);
                // We continue processing other files even if one fails
            }
        }

        return results;
    }

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
        
        // Calculate Financials according to Architecture Doc rules
        Company company = ticket.getCompany();
        GstConfig gstConfig = gstConfigRepository.findActiveConfigForDate(ticket.getTravelDate())
                .orElseThrow(() -> new BusinessException("No active GST config found for travel date", "MISSING_GST_CONFIG", HttpStatus.INTERNAL_SERVER_ERROR));
        
        // 1. Service Charge = Base Fare × company.serviceChargePct
        BigDecimal serviceCharge = request.getBaseFare()
                .multiply(company.getServiceChargePct().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        ticket.setServiceCharge(serviceCharge);
        
        // 2. CGST = Service Charge × gstConfig.cgstRate
        BigDecimal cgst = serviceCharge
                .multiply(gstConfig.getCgstRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        ticket.setCgst(cgst);
        
        // 3. SGST = Service Charge × gstConfig.sgstRate
        BigDecimal sgst = serviceCharge
                .multiply(gstConfig.getSgstRate().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        ticket.setSgst(sgst);
        
        // 4. Total = Base Fare + Service Charge + CGST + SGST
        BigDecimal total = request.getBaseFare().add(serviceCharge).add(cgst).add(sgst);
        ticket.setTotalAmount(total);

        ticket.setStatus(TicketStatus.APPROVED);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        TicketDto.TicketResponse newValue = mapToResponse(savedTicket);
        
        auditService.logAction("TICKET", savedTicket.getId(), "APPROVED", oldValue, newValue, user);
        
        return newValue;
    }

    @Transactional
    public void deleteTicket(Long id, User user) {
        Ticket ticket = getTicketEntity(id);
        
        if (ticket.getStatus() == TicketStatus.BILLED || ticket.getStatus() == TicketStatus.PAID) {
            throw new BusinessException("Cannot delete a billed ticket", "TICKET_LOCKED", HttpStatus.FORBIDDEN);
        }
        
        auditService.logAction("TICKET", ticket.getId(), "DELETED", mapToResponse(ticket), null, user);
        
        // Delete physical file
        if (ticket.getFilePath() != null) {
            fileStorageService.deleteFile(ticket.getFilePath());
        }
        
        ticketRepository.delete(ticket);
    }

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
        response.setCreatedById(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null);
        return response;
    }
}
