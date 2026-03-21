package com.travelbillpro.service;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Detects the structure layout of a travel PDF to route extraction correctly.
 *
 * BUG C fix: Updated signal detection to include city names, seat types, and
 * updated keyword lists. Added normalizeForSearch() to ensure keywords match
 * cleaned page text.
 */
@Component
public class PdfStructureAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PdfStructureAnalyzer.class);

    public enum PdfStructure {
        SINGLE_PASSENGER,
        GROUP_BOOKING_INDIGO,
        MULTI_LEG_ITINERARY,
        UNKNOWN
    }

    // ── Fare page detection — any ONE match = this is a fare page
    private static final List<String> FARE_PAGE_SIGNALS = List.of(
        "total charge", "total fare", "grand total", "amount payable",
        "fare details", "airfare charges", "airfare charge",
        "base fare", "aviation security", "user development fee",
        "igst", "cgst", "sgst", "baggage information",
        "total amount", "net amount", "invoice amount"
    );

    // ── Passenger page detection — any ONE match = this is a passenger page
    // BUG C fix: Added city names, seat types, flight codes
    private static final List<String> PASSENGER_PAGE_SIGNALS = List.of(
        "passenger information", "passenger name",
        "pnr", "booking ref", "confirmed",
        "departing flight", "sector",
        "check-in", "check in",
        "window", "aisle", "middle",          // seat types
        "indigo", "spicejet", "air india",    // carriers
        "6e", "sg", "ai", "uk",               // flight prefixes
        "rpr", "del", "hyd", "bom", "maa",   // IATA codes
        "vns", "ccu", "blr", "ixr", "nag",
        "raipur", "delhi", "mumbai", "hyderabad",  // city names
        "varanasi", "bangalore", "nagpur",
        "adult", "male", "female"              // passenger descriptors
    );

    /**
     * Analyze PDF pages to determine structure type.
     *
     * Returns the inferred structure based on:
     * - Last page content (is it a fare summary?)
     * - Count of passenger-only pages (1 to N-1)
     * - Total page count
     *
     * BUG C fix: Now calls normalizeForSearch() on page text before keyword matching.
     *
     * @param content PdfTextExtractorService.PdfContent with cleaned pages
     * @return Detected structure enum
     */
    public PdfStructure analyze(TicketParserService.PdfContent content) {
        int total = content.totalPages();
        log.debug("Analyzing PDF structure: {} pages", total);

        if (total == 1) {
            return PdfStructure.SINGLE_PASSENGER;
        }

        // Analyze LAST page — is it a fare summary page?
        String lastPageText = normalizeForSearch(content.pageText(total));
        boolean lastPageIsFare = FARE_PAGE_SIGNALS.stream()
                .anyMatch(lastPageText::contains);

        log.debug("Last page ({}): isFare={}, preview='{}'",
                total, lastPageIsFare,
                lastPageText.length() > 80 ? lastPageText.substring(0, 80) : lastPageText);

        if (!lastPageIsFare) {
            // Last page has no fare signals — not a group booking format
            return total <= 2 ? PdfStructure.SINGLE_PASSENGER : PdfStructure.UNKNOWN;
        }

        // Count passenger-only pages (1 to N-1)
        long passengerPageCount = 0;
        for (int i = 1; i < total; i++) {
            String pageText = normalizeForSearch(content.pageText(i));
            boolean hasPassengerSignal = PASSENGER_PAGE_SIGNALS.stream()
                    .anyMatch(pageText::contains);
            boolean hasFareSignal = FARE_PAGE_SIGNALS.stream()
                    .anyMatch(pageText::contains);

            if (hasPassengerSignal && !hasFareSignal) {
                passengerPageCount++;
                log.debug("Page {} = PASSENGER (signals found, no fare)", i);
            } else {
                log.debug("Page {} = OTHER (passenger={}, fare={})", 
                        i, hasPassengerSignal, hasFareSignal);
            }
        }

        log.info("Structure analysis: {} total pages, {} passenger-only pages, " +
                 "last page is fare={}", total, passengerPageCount, lastPageIsFare);

        // GROUP: at least 1 passenger-only page + fare on last page
        // (Changed from >= 2 to >= 1 to catch 2-passenger PDFs)
        if (passengerPageCount >= 1) {
            log.info("Detected GROUP_BOOKING_INDIGO structure");
            return PdfStructure.GROUP_BOOKING_INDIGO;
        }

        return PdfStructure.SINGLE_PASSENGER;
    }

    /**
     * Normalize text for keyword matching.
     * BUG C fix: Added method to ensure consistent text preprocessing.
     *
     * - Convert to lowercase
     * - Strip non-ASCII and non-printable
     * - Collapse whitespace
     * - Trim
     *
     * @param text Raw page text
     * @return Normalized text ready for keyword search
     */
    private String normalizeForSearch(String text) {
        if (text == null || text.isBlank()) return "";
        return text
            .replaceAll("[^\\x20-\\x7E\\n]", " ")  // remove non-printable
            .toLowerCase()
            .replaceAll("\\s+", " ")                // collapse whitespace
            .trim();
    }
}
