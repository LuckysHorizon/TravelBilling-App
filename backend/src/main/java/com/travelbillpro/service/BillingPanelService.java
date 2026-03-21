package com.travelbillpro.service;

import com.travelbillpro.dto.BillingPanelDto;
import com.travelbillpro.dto.InvoiceDto;
import com.travelbillpro.dto.TicketDto;
import com.travelbillpro.entity.BillingPanel;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.BillingPanelRepository;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingPanelService {

    private final BillingPanelRepository panelRepository;
    private final CompanyRepository companyRepository;
    private final TicketRepository ticketRepository;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public List<BillingPanelDto.PanelResponse> getAllPanels() {
        return panelRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillingPanelDto.PanelResponse> getPanelsByCompany(Long companyId) {
        return panelRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillingPanelDto.PanelResponse getPanelById(Long id) {
        return mapToResponse(getPanelEntity(id));
    }

    @Transactional
    public BillingPanelDto.PanelResponse createPanel(BillingPanelDto.CreatePanelRequest request, User user) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        BillingPanel panel = new BillingPanel();
        panel.setLabel(request.getLabel());
        panel.setCompany(company);
        panel.setStatus(BillingPanel.BillingPanelStatus.OPEN);
        panel.setCreatedBy(user);

        return mapToResponse(panelRepository.save(panel));
    }

    @Transactional
    public BillingPanelDto.PanelResponse addTickets(Long panelId, List<Long> ticketIds, User user) {
        BillingPanel panel = getPanelEntity(panelId);

        if (panel.getStatus() != BillingPanel.BillingPanelStatus.OPEN) {
            throw new BusinessException("Panel is not open for modifications", "PANEL_CLOSED", HttpStatus.BAD_REQUEST);
        }

        for (Long ticketId : ticketIds) {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new BusinessException("Ticket " + ticketId + " not found", "TICKET_NOT_FOUND", HttpStatus.NOT_FOUND));

            // Must be APPROVED and belong to same company
            if (ticket.getStatus() != TicketStatus.APPROVED) {
                log.warn("Skipping ticket {} — status is {}, must be APPROVED", ticketId, ticket.getStatus());
                continue;
            }
            if (!ticket.getCompany().getId().equals(panel.getCompany().getId())) {
                log.warn("Skipping ticket {} — belongs to different company", ticketId);
                continue;
            }
            if (ticket.getBillingPanel() != null) {
                log.warn("Skipping ticket {} — already assigned to panel {}", ticketId, ticket.getBillingPanel().getId());
                continue;
            }

            ticket.setBillingPanel(panel);
            ticketRepository.save(ticket);
        }

        return mapToResponse(panelRepository.findById(panelId).orElseThrow());
    }

    @Transactional
    public BillingPanelDto.PanelResponse removeTicket(Long panelId, Long ticketId) {
        BillingPanel panel = getPanelEntity(panelId);

        if (panel.getStatus() != BillingPanel.BillingPanelStatus.OPEN) {
            throw new BusinessException("Panel is not open for modifications", "PANEL_CLOSED", HttpStatus.BAD_REQUEST);
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Ticket not found", "TICKET_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (ticket.getBillingPanel() != null && ticket.getBillingPanel().getId().equals(panelId)) {
            ticket.setBillingPanel(null);
            ticketRepository.save(ticket);
        }

        return mapToResponse(panelRepository.findById(panelId).orElseThrow());
    }

    @Transactional
    public InvoiceDto.InvoiceResponse generateInvoiceFromPanel(Long panelId, User user) {
        BillingPanel panel = getPanelEntity(panelId);

        if (panel.getStatus() != BillingPanel.BillingPanelStatus.OPEN) {
            throw new BusinessException("Panel is already invoiced or closed", "PANEL_NOT_OPEN", HttpStatus.BAD_REQUEST);
        }

        List<Ticket> tickets = ticketRepository.findByBillingPanelId(panelId);
        if (tickets.isEmpty()) {
            throw new BusinessException("No tickets in this panel", "EMPTY_PANEL", HttpStatus.BAD_REQUEST);
        }

        // Determine date range from tickets
        LocalDate minDate = tickets.stream().map(Ticket::getTravelDate).filter(d -> d != null).min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate maxDate = tickets.stream().map(Ticket::getTravelDate).filter(d -> d != null).max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        InvoiceDto.GenerateInvoiceRequest invoiceRequest = new InvoiceDto.GenerateInvoiceRequest();
        invoiceRequest.setCompanyId(panel.getCompany().getId());
        invoiceRequest.setStartDate(minDate);
        invoiceRequest.setEndDate(maxDate);

        InvoiceDto.InvoiceResponse invoiceResponse = invoiceService.generateInvoice(invoiceRequest, user);

        panel.setStatus(BillingPanel.BillingPanelStatus.INVOICED);
        panelRepository.save(panel);

        return invoiceResponse;
    }

    @Transactional
    public void deletePanel(Long id) {
        BillingPanel panel = getPanelEntity(id);

        if (panel.getStatus() == BillingPanel.BillingPanelStatus.INVOICED) {
            throw new BusinessException("Cannot delete an invoiced panel", "PANEL_INVOICED", HttpStatus.BAD_REQUEST);
        }

        // Unlink tickets
        List<Ticket> tickets = ticketRepository.findByBillingPanelId(id);
        for (Ticket t : tickets) {
            t.setBillingPanel(null);
            ticketRepository.save(t);
        }

        panelRepository.delete(panel);
    }

    // ═══════ HELPERS ═══════

    private BillingPanel getPanelEntity(Long id) {
        return panelRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Billing panel not found", "PANEL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private BillingPanelDto.PanelResponse mapToResponse(BillingPanel panel) {
        BillingPanelDto.PanelResponse resp = new BillingPanelDto.PanelResponse();
        resp.setId(panel.getId());
        resp.setLabel(panel.getLabel());
        resp.setCompanyId(panel.getCompany().getId());
        resp.setCompanyName(panel.getCompany().getName());
        resp.setStatus(panel.getStatus().name());
        resp.setCreatedAt(panel.getCreatedAt());

        if (panel.getInvoice() != null) {
            resp.setInvoiceId(panel.getInvoice().getId());
            resp.setInvoiceNumber(panel.getInvoice().getInvoiceNumber());
        }

        // Load tickets
        List<Ticket> tickets = ticketRepository.findByBillingPanelId(panel.getId());
        resp.setTicketCount(tickets.size());

        BigDecimal total = tickets.stream()
                .map(t -> t.getBaseFare() != null ? t.getBaseFare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(total);

        resp.setTickets(tickets.stream().map(t -> {
            TicketDto.TicketResponse tr = new TicketDto.TicketResponse();
            tr.setId(t.getId());
            tr.setPnrNumber(t.getPnrNumber());
            tr.setPassengerName(t.getPassengerName());
            tr.setTravelDate(t.getTravelDate());
            tr.setBaseFare(t.getBaseFare());
            tr.setTotalAmount(t.getTotalAmount());
            tr.setStatus(t.getStatus());
            tr.setOrigin(t.getOrigin());
            tr.setDestination(t.getDestination());
            return tr;
        }).collect(Collectors.toList()));

        return resp;
    }
}
