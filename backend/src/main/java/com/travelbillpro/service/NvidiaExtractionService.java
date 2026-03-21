package com.travelbillpro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.travelbillpro.config.NvidiaConfig;
import com.travelbillpro.exception.NvidiaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * UNIFIED Extraction Service — One prompt, one schema, two methods.
 *
 * This service implements the complete extraction pipeline consolidation:
 * - extractFromText(): For PDFs with text layers (send all pages joined)
 * - extractFromImages(): For scanned/image-only PDFs (vision model fallback)
 * - isEmptyResponse(): Detect semantic failure (AI returned nulls despite input)
 *
 * Rate-limited to match NVIDIA NIM 38 req/min.
 * Retries on 429/503 with exponential backoff.
 *
 * The system prompt teaches the model to handle all PDF types in one call:
 * - Single passenger bookings
 * - Group bookings (multiple passengers, single PNR)
 * - Multi-leg itineraries
 */
@Service
public class NvidiaExtractionService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaExtractionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ────────────────────────────────────────────────────────────────────────
    //  UNIFIED SYSTEM PROMPT
    // ────────────────────────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
You are a travel invoice parser for a financial billing system.
Your job is to read a travel document (flight or bus ticket) and extract
structured data. Return ONLY a raw JSON array. No markdown. No explanation.
No code fences. Just the array.

OUTPUT RULES:
1. ONE RECORD PER PASSENGER. 8 passengers = 8 objects. 1 passenger = 1 object.
2. All passengers share same PNR, route, date, operator.
3. FARE: Find grand total on any page (often last page).
   If no individual fare shown: base_fare = total_base_fare / passenger_count
   total_amount = grand_total / passenger_count  (round to 2 decimal places)
4. base_fare = airfare/base only. total_amount = final including all taxes.
5. travel_date format: yyyy-MM-dd (convert "13 Feb 2025" to "2025-02-13")
6. pnr_number: max 20 chars, alphanumeric, exactly as printed.
7. Null any field you cannot find. Never guess. Fare is often on the last page.

SCHEMA — return exactly this per passenger:
[{"pnr_number":"","passenger_name":"","travel_date":"yyyy-MM-dd",
  "operator_name":"","origin":"","destination":"",
  "base_fare":0.00,"total_amount":0.00,"ticket_type":"FLIGHT","confidence":0.0}]
""";

    // ────────────────────────────────────────────────────────────────────────
    //  UNIFIED EXTRACTION SCHEMA
    // ────────────────────────────────────────────────────────────────────────

    private static final Map<String, Object> TICKET_EXTRACTION_SCHEMA = Map.of(
        "type", "array",
        "minItems", 1,
        "items", Map.of(
            "type", "object",
            "required", List.of(
                "pnr_number", "passenger_name", "travel_date",
                "origin", "destination", "total_amount", "ticket_type"
            ),
            "properties", Map.of(
                "pnr_number",     Map.of("type", "string", "maxLength", 20),
                "passenger_name", Map.of("type", "string"),
                "travel_date",    Map.of("type", "string",
                                        "pattern", "^\\d{4}-\\d{2}-\\d{2}$"),
                "operator_name",  Map.of("type", List.of("string", "null")),
                "origin",         Map.of("type", "string"),
                "destination",    Map.of("type", "string"),
                "base_fare",      Map.of("type", List.of("number", "null")),
                "total_amount",   Map.of("type", "number", "minimum", 0),
                "ticket_type",    Map.of("type", "string",
                                        "enum", List.of("FLIGHT", "BUS", "TRAIN")),
                "confidence",     Map.of("type", "number",
                                        "minimum", 0.0, "maximum", 1.0)
            )
        )
    );

    // ────────────────────────────────────────────────────────────────────────
    //  RESULT RECORD FOR AUDIT
    // ────────────────────────────────────────────────────────────────────────

    public record ExtractionCallResult(
        String rawJson,
        List<Map<String, Object>> parsedRecords,
        String modelUsed,
        int promptTokens,
        int completionTokens,
        long processingMs
    ) {}

    private final NvidiaConfig config;
    private final RateLimiter rateLimiter;
    private final HttpClient http;

    public NvidiaExtractionService(NvidiaConfig config,
            @Qualifier("nvidiaRateLimiter") RateLimiter rateLimiter) {
        this.config      = config;
        this.rateLimiter = rateLimiter;
        this.http        = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Extract from cleaned text (all pages as one string).
     * Sends to text model with guided_json schema.
     *
     * @param cleanedText Joined text from all pages
     * @return ExtractionCallResult with parsed records
     * @throws Exception on API failure
     */
    public ExtractionCallResult extractFromText(String cleanedText) {
        log.info("Calling NVIDIA NIM (text mode, model={})", config.getModelText());
        rateLimiter.acquire();

        Map<String, Object> body = buildRequestBody(
            config.getModelText(),
            SYSTEM_PROMPT,
            "Extract all travel tickets from this document:\n\n" + cleanedText,
            TICKET_EXTRACTION_SCHEMA
        );

        return execute(body, config.getModelText());
    }

    /**
     * Extract from rendered page images (vision fallback).
     * Sends to vision model without guided_json.
     *
     * @param base64Pages List of PNG images as base64 strings
     * @return ExtractionCallResult with parsed records
     * @throws Exception on API failure
     */
    public ExtractionCallResult extractFromImages(List<String> base64Pages) {
        log.info("Calling NVIDIA NIM (vision mode, model={}, pages={})",
                config.getModelVision(), base64Pages.size());
        rateLimiter.acquire();

        List<Map<String, Object>> contentParts = new ArrayList<>();
        for (int i = 0; i < base64Pages.size(); i++) {
            contentParts.add(Map.of(
                "type", "image_url",
                "image_url", Map.of(
                    "url", "data:image/png;base64," + base64Pages.get(i)
                )
            ));
            contentParts.add(Map.of(
                "type", "text",
                "text", "Page " + (i + 1) + " of the travel document."
            ));
        }
        contentParts.add(Map.of(
            "type", "text",
            "text", "Extract all travel ticket details from these pages. " +
                    "Apply the rules in the system prompt."
        ));

        Map<String, Object> body = buildRequestBody(
            config.getModelVision(),
            SYSTEM_PROMPT,
            contentParts,    // vision: content is a List, not a String
            null             // vision model does not support guided_json
        );

        return execute(body, config.getModelVision());
    }

    /**
     * Check if response is semantically empty.
     * The model may return HTTP 200 with valid JSON but all-null fields
     * and confidence=0.0 when it fails to extract any data.
     *
     * @param records Parsed record list from ExtractionCallResult
     * @return true if all critical fields are null or confidence < 0.1
     */
    public boolean isEmptyResponse(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }
        Map<String, Object> first = records.get(0);
        boolean pnrNull  = first.get("pnr_number")     == null;
        boolean nameNull = first.get("passenger_name")  == null;
        boolean fromNull = first.get("origin")          == null;
        boolean toNull   = first.get("destination")     == null;
        boolean fareNull = first.get("total_amount")    == null;
        Object conf      = first.get("confidence");
        double confidence = conf instanceof Number n ? n.doubleValue() : 0.0;

        // Empty if all critical fields are null OR confidence < 0.1
        return (pnrNull && nameNull && fromNull && toNull && fareNull) 
               || confidence < 0.1;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Build & Execute
    // ────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildRequestBody(String model,
                                                   String systemPrompt,
                                                   Object userContent,
                                                   Object guidedJsonSchema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",       model);
        body.put("max_tokens",  4096);
        body.put("temperature", 0.0);
        body.put("stream",      false);
        body.put("messages", List.of(
            Map.of("role", "system",  "content", systemPrompt),
            Map.of("role", "user",    "content", userContent)
        ));
        if (guidedJsonSchema != null) {
            body.put("nvext", Map.of("guided_json", guidedJsonSchema));
        }
        return body;
    }

    private ExtractionCallResult execute(Map<String, Object> body, String model) {
        int  maxAttempts = 3;
        long delayMs     = 2000;
        String chatUrl   = config.getBaseUrl() + "/chat/completions";

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                String reqJson = MAPPER.writeValueAsString(body);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatUrl))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();

                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString());
                long elapsed = System.currentTimeMillis() - start;

                if (response.statusCode() == 200) {
                    log.info("NVIDIA NIM responded in {}ms (attempt {})", elapsed, attempt);
                    return parseResponse(response.body(), model, elapsed);
                }
                if (response.statusCode() == 429 || response.statusCode() == 503) {
                    log.warn("NVIDIA NIM {} on attempt {} — retrying in {}ms",
                            response.statusCode(), attempt, delayMs);
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                    continue;
                }
                throw new NvidiaApiException(
                    "NVIDIA NIM HTTP " + response.statusCode() + ": " + response.body());

            } catch (NvidiaApiException e) {
                throw e;  // re-throw directly
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NvidiaApiException("Interrupted during retry");
            } catch (Exception e) {
                throw new NvidiaApiException("NVIDIA NIM call failed: " + e.getMessage(), e);
            }
        }
        throw new NvidiaApiException("NVIDIA NIM failed after " + maxAttempts + " attempts");
    }

    @SuppressWarnings("unchecked")
    private ExtractionCallResult parseResponse(String rawJson, String model, long ms) {
        try {
            Map<?, ?> root = MAPPER.readValue(rawJson, Map.class);
            List<Map<?, ?>> choices = (List<Map<?, ?>>) root.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new NvidiaApiException("No choices in NVIDIA response");
            }

            Map<?, ?> msg  = (Map<?, ?>) choices.get(0).get("message");
            String content = (String) msg.get("content");

            // Strip accidental markdown fences
            String clean = content.strip();
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
                             .replaceAll("```\\s*$", "").strip();
            }

            Object usageObj = root.get("usage");
            Map<?, ?> usage = usageObj instanceof Map<?, ?> ? (Map<?, ?>) usageObj : Map.of();
            int promptTok     = toInt(usage.get("prompt_tokens"));
            int completionTok = toInt(usage.get("completion_tokens"));

            log.debug("NVIDIA content ({} chars, {}+{} tokens): {}",
                    clean.length(), promptTok, completionTok,
                    clean.length() > 200 ? clean.substring(0, 200) + "..." : clean);

            List<Map<String, Object>> records = MAPPER.readValue(clean,
                    new TypeReference<List<Map<String, Object>>>() {});

            log.info("Parsed {} passenger record(s) from NVIDIA response", records.size());
            return new ExtractionCallResult(rawJson, records, model, promptTok, completionTok, ms);
        } catch (NvidiaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new NvidiaApiException("Failed to parse NVIDIA response: " + e.getMessage(), e);
        }
    }

    private int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
