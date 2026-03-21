package com.travelbillpro.service;

import com.travelbillpro.entity.Ticket;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.enums.TicketType;
import com.travelbillpro.exception.ExtractionException;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Owns the ONLY @Transactional boundary for ticket persistence.
 *
 * This class has exactly one responsibility: write tickets to the database
 * in a single atomic transaction.
 *
 * It is called ONLY after AI extraction is complete — never during I/O.
 * If it fails, it rolls back cleanly with no side effects on audit.
 *
 * CRITICAL INVARIANT: This service is instantiated fresh for each request.
 * No @Transactional method calls another @Transactional method in this service.
 * No @Transactional method in this service calls any I/O method (HTTP, file, etc).
 *
 * FIX #7: Use sentinel defaults instead of throwing on partial data.
 *         Always persist tickets, marking partial extractions as PENDING_REVIEW.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketPersistenceService {

    private final TicketRepository ticketRepository;

    /**
     * Saves all tickets atomically in a single transaction.
     *
     * FIX #7: REWRITTEN to never throw on partial data.
     *
     * Strategy:
     * - Check for truly unsaveable states (null ticket_type, null company_id)
     * - For all other null/blank fields from AI, apply sentinel defaults
     * - Always mark tickets with incomplete data as PENDING_REVIEW
     * - Persist to DB so humans can review and correct
     *
     * @param tickets List of tickets to persist
     * @return Persisted tickets with database IDs
     * @throws ExtractionException only if truly unsaveable (empty list, null type, etc)
     */
    @Transactional
    public List<Ticket> saveAll(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            throw new ExtractionException("Cannot persist empty ticket list");
        }

        // Sanitize and prepare each ticket for persistence
        for (Ticket ticket : tickets) {
            sanitizeTicket(ticket);
        }

        List<Ticket> saved = ticketRepository.saveAll(tickets);
        long pendingReviewCount = saved.stream()
                .filter(t -> t.getStatus() == TicketStatus.PENDING_REVIEW)
                .count();
        
        log.info("Persisted {} ticket(s) ({} PENDING_REVIEW)", saved.size(), pendingReviewCount);
        return saved;
    }

    /**
     * Update status only — used by approve/reject endpoints.
     * Also @Transactional to ensure clean commit.
     */
    @Transactional
    public Ticket updateStatus(Long id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ExtractionException("Ticket not found: " + id));
        ticket.setStatus(TicketStatus.valueOf(status));
        return ticketRepository.save(ticket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sanitization — apply sentinel defaults (FIX #7)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate a sentinel PNR that ALWAYS fits VARCHAR(20).
     * BUG A fix: "UNKNOWN-" + timestamp was 22 chars, causing crash.
     * Now uses "UNK-" + last 8 digits of epoch, max 12 chars.
     *
     * @return sentinel PNR: "UNK-" + 8 digits, max 12 characters
     */
    private static String sentinelPnr() {
        String ts = String.valueOf(System.currentTimeMillis());
        String lastEight = ts.substring(ts.length() - 8);
        return "UNK-" + lastEight;  // e.g., "UNK-01270030" = 12 chars
    }

    /**
     * Safely truncate string values to max length.
     * BUG A fix: prevents any varchar field overflow crash.
     * If value exceeds maxLen, truncates silently; doesn't throw.
     *
     * @param value The string to cap
     * @param maxLen Maximum allowed length
     * @return Truncated value if needed, or original value if within limit
     */
    private static String cap(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    /**
     * Prepares a ticket for database persistence by applying sentinel defaults
     * to any null/blank fields that come from AI extraction failures.
     *
     * FIX #7: Use sentinel defaults instead of throwing. Mark incomplete
     * extractions as PENDING_REVIEW so humans can review and correct.
     *
     * BUG A: All string field assignments now wrapped in cap() to prevent
     * VARCHAR overflow crashes. For example, if AI returns a 30-char PNR,
     * it gets truncated to 20 chars silently.
     *
     * @param ticket The ticket to sanitize
     * @throws ExtractionException only if truly unsalvageable
     */
    private void sanitizeTicket(Ticket ticket) {
        // HARD BLOCKS: These are schema NOT NULL fields that cannot have defaults
        requireForDb(ticket.getTicketType() != null,
                "ticket_type cannot be null");
        requireForDb(ticket.getCompany() != null,
                "company_id cannot be null");

        boolean hasIncompleteData = false;

        // PNR: if null/blank, use sentinel and mark for review
        // BUG A: sentinel is max 12 chars, always fits VARCHAR(20)
        if (ticket.getPnrNumber() == null || ticket.getPnrNumber().isBlank()) {
            ticket.setPnrNumber(cap(sentinelPnr(), 20));
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("PNR null/blank – set sentinel {}, marked PENDING_REVIEW", 
                     ticket.getPnrNumber());
        } else {
            // PNR is not null, but cap it anyway in case AI returned too-long value
            ticket.setPnrNumber(cap(ticket.getPnrNumber(), 20));
        }

        // Passenger name: if null/blank, use sentinel
        if (ticket.getPassengerName() == null || ticket.getPassengerName().isBlank()) {
            ticket.setPassengerName(cap("UNKNOWN PASSENGER", 255));
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Passenger name null/blank – set sentinel, marked PENDING_REVIEW");
        } else {
            ticket.setPassengerName(cap(ticket.getPassengerName(), 255));
        }

        // Origin: if null/blank, use sentinel
        if (ticket.getOrigin() == null || ticket.getOrigin().isBlank()) {
            ticket.setOrigin(cap("UNKNOWN", 255));
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Origin null/blank – set sentinel UNKNOWN, marked PENDING_REVIEW");
        } else {
            ticket.setOrigin(cap(ticket.getOrigin(), 255));
        }

        // Destination: if null/blank, use sentinel
        if (ticket.getDestination() == null || ticket.getDestination().isBlank()) {
            ticket.setDestination(cap("UNKNOWN", 255));
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Destination null/blank – set sentinel UNKNOWN, marked PENDING_REVIEW");
        } else {
            ticket.setDestination(cap(ticket.getDestination(), 255));
        }

        // Operator name: nullable, but cap if present
        if (ticket.getOperatorName() != null && !ticket.getOperatorName().isBlank()) {
            ticket.setOperatorName(cap(ticket.getOperatorName(), 255));
        }

        // Travel date: if null, use sentinel epoch
        if (ticket.getTravelDate() == null) {
            ticket.setTravelDate(LocalDate.of(1970, 1, 1));
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Travel date null – set sentinel 1970-01-01, marked PENDING_REVIEW");
        }

        // Ticket type: already checked above, but ensure it's not null
        if (ticket.getTicketType() == null) {
            ticket.setTicketType(TicketType.UNKNOWN);
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Ticket type null – set UNKNOWN, marked PENDING_REVIEW");
        }

        // Base fare: if null, use zero with review flag
        if (ticket.getBaseFare() == null) {
            ticket.setBaseFare(BigDecimal.ZERO);
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
            hasIncompleteData = true;
            log.warn("Base fare null – set ZERO, marked PENDING_REVIEW");
        } else if (ticket.getBaseFare().compareTo(BigDecimal.ZERO) < 0) {
            throw new ExtractionException("Negative fare for ticket " + ticket.getPnrNumber());
        }

        // Service charge, CGST, SGST: if null, use zero
        if (ticket.getServiceCharge() == null) {
            ticket.setServiceCharge(BigDecimal.ZERO);
        }
        if (ticket.getCgst() == null) {
            ticket.setCgst(BigDecimal.ZERO);
        }
        if (ticket.getSgst() == null) {
            ticket.setSgst(BigDecimal.ZERO);
        }

        // Total amount: if null, calculate or use base fare
        if (ticket.getTotalAmount() == null) {
            ticket.setTotalAmount(ticket.getBaseFare() != null
                    ? ticket.getBaseFare()
                    : BigDecimal.ZERO);
        }

        // Status: if still null (not set to PENDING_REVIEW above), default to PENDING_REVIEW
        if (ticket.getStatus() == null) {
            ticket.setStatus(TicketStatus.PENDING_REVIEW);
        }

        // AI confidence: if null, set to zero (low confidence)
        if (ticket.getAiConfidence() == null) {
            ticket.setAiConfidence(BigDecimal.ZERO);
        }

        if (hasIncompleteData) {
            log.warn("Ticket {} has incomplete AI extraction data but will be persisted for human review",
                    ticket.getPnrNumber());
        }
    }

    private void requireForDb(boolean condition, String message) {
        if (!condition) throw new ExtractionException("DB constraint violation: " + message);
    }
}
