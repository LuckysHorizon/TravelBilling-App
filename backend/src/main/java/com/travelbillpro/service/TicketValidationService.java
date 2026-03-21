package com.travelbillpro.service;

import com.travelbillpro.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates tickets WITHOUT throwing exceptions.
 *
 * This component returns a ValidationReport with lists of errors and warnings.
 * All null checks are explicit — no NPE is possible.
 *
 * CRITICAL: This validator NEVER throws. Even if a field is completely null,
 * it records an error and continues.
 *
 * This design ensures that validation errors do NOT poison a transaction.
 * It is safe to call this from anywhere, in or out of a transaction.
 */
@Component
@Slf4j
public class TicketValidationService {

    private static final BigDecimal FARE_TOLERANCE      = new BigDecimal("0.50");
    private static final BigDecimal MAX_FARE_INR        = new BigDecimal("500000");
    private static final double     CONFIDENCE_THRESHOLD = 0.75;
    private static final Pattern    PNR_PATTERN =
            Pattern.compile("^[A-Z0-9]{5,10}$");

    /**
     * Immutable report of validation results.
     */
    public record ValidationReport(
        boolean isValid,
        List<String> errors,
        List<String> warnings
    ) {}

    /**
     * NEVER throws. Returns a report with errors/warnings lists.
     * All null checks are explicit — no NPE possible.
     *
     * @param ticket The ticket to validate (can be null, will record error)
     * @return A validation report with isValid, errors, and warnings
     */
    public ValidationReport validate(Ticket ticket) {
        if (ticket == null) {
            return new ValidationReport(false,
                List.of("NULL_TICKET"), List.of());
        }

        List<String> errors   = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // ─ PNR ───────────────────────────────────────────────────────────
        if (isBlank(ticket.getPnrNumber())) {
            errors.add("MISSING_PNR");
        } else if (!PNR_PATTERN.matcher(ticket.getPnrNumber().trim().toUpperCase()).matches()) {
            errors.add("INVALID_PNR_FORMAT: '" + ticket.getPnrNumber() + "'");
        }

        // ─ Passenger Name ────────────────────────────────────────────────
        if (isBlank(ticket.getPassengerName())) {
            errors.add("MISSING_PASSENGER_NAME");
        }

        // ─ Origin / Destination ──────────────────────────────────────────
        if (isBlank(ticket.getOrigin()))      errors.add("MISSING_ORIGIN");
        if (isBlank(ticket.getDestination())) errors.add("MISSING_DESTINATION");

        if (!isBlank(ticket.getOrigin()) && !isBlank(ticket.getDestination())
                && ticket.getOrigin().equalsIgnoreCase(ticket.getDestination())) {
            errors.add("ORIGIN_EQUALS_DESTINATION");
        }

        // ─ Travel Date ───────────────────────────────────────────────────
        if (ticket.getTravelDate() == null) {
            errors.add("MISSING_TRAVEL_DATE");
        } else {
            LocalDate travel = ticket.getTravelDate();
            LocalDate now    = LocalDate.now();
            if (travel.isBefore(now.minusYears(3)))
                warnings.add("TRAVEL_DATE_OLDER_THAN_3_YEARS: " + travel);
            if (travel.isAfter(now.plusYears(2)))
                warnings.add("TRAVEL_DATE_FAR_FUTURE: " + travel);
        }

        // ─ Base Fare ─────────────────────────────────────────────────────
        if (ticket.getBaseFare() == null) {
            errors.add("MISSING_BASE_FARE");
        } else {
            if (ticket.getBaseFare().compareTo(BigDecimal.ZERO) <= 0)
                errors.add("ZERO_OR_NEGATIVE_FARE: " + ticket.getBaseFare());
            if (ticket.getBaseFare().compareTo(MAX_FARE_INR) > 0)
                warnings.add("UNUSUALLY_HIGH_FARE: " + ticket.getBaseFare());
        }

        // ─ Confidence Score ─────────────────────────────────────────────
        if (ticket.getAiConfidence() != null) {
            if (ticket.getAiConfidence().doubleValue() < CONFIDENCE_THRESHOLD) {
                warnings.add("LOW_CONFIDENCE_SCORE: " +
                        ticket.getAiConfidence().toPlainString());
            }
        }

        // ─ Status ────────────────────────────────────────────────────────
        if (ticket.getStatus() == null) {
            errors.add("MISSING_STATUS");
        }

        return new ValidationReport(errors.isEmpty(), errors, warnings);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
