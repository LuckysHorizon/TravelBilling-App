package com.travelbillpro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.dto.TicketExtractionResult;
import com.travelbillpro.enums.TicketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExtractionService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    private final RegexExtractionService regexExtractionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";

    public TicketExtractionResult extractTicketData(String rawText, TicketType expectedType) {
        if (apiKey == null || apiKey.trim().isEmpty() || "${GEMINI_API_KEY:}".equals(apiKey)) {
            log.warn("Gemini API key not configured, falling back to Regex extraction.");
            return regexExtractionService.fallbackExtraction(rawText, expectedType);
        }

        try {
            String prompt = buildPrompt(rawText, expectedType);
            String url = GEMINI_API_URL + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            contents.put("role", "user");
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", new Object[]{parts});
            requestBody.put("contents", new Object[]{contents});

            Map<String, Object> config = new HashMap<>();
            config.put("temperature", 0.1); // Low temp for extraction consistency
            config.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", config);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("Calling Gemini API for extraction...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseGeminiResponse(response.getBody(), expectedType, rawText);
            } else {
                log.error("Gemini API returned error: {}", response.getStatusCode());
                return regexExtractionService.fallbackExtraction(rawText, expectedType);
            }

        } catch (Exception e) {
            log.error("Failed to extract data via Gemini API", e);
            return regexExtractionService.fallbackExtraction(rawText, expectedType);
        }
    }

    private String buildPrompt(String rawText, TicketType type) {
        return """
            You are a highly accurate data extraction API. Extract the following fields from this %s ticket text.
            If a field is not found, leave it null.
            Return ONLY a valid JSON object with EXACTLY this structure, no markdown, no other text:
            {
              "pnrNumber": "value",
              "pnrConfidence": number between 0 and 100,
              "passengerName": "value",
              "passengerConfidence": number between 0 and 100,
              "travelDate": "YYYY-MM-DD",
              "dateConfidence": number between 0 and 100,
              "baseFare": number,
              "amountConfidence": number between 0 and 100,
              "origin": "value",
              "destination": "value",
              "operatorName": "value"
            }
            
            Text to extract from:
            ---
            %s
            ---
            """.formatted(type.name(), rawText);
    }

    private TicketExtractionResult parseGeminiResponse(String responseBody, TicketType type, String rawText) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Navigate Gemini's response structure: candidates[0].content.parts[0].text
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String jsonText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                
                // Clean markdown wrapping if Gemini ignored instructions
                jsonText = jsonText.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
                
                JsonNode extracted = objectMapper.readTree(jsonText);
                
                TicketExtractionResult result = new TicketExtractionResult();
                result.setExtractionMethod("AI");
                result.setTicketType(type);
                
                // Parse fields safely
                if (extracted.hasNonNull("pnrNumber")) result.setPnrNumber(extracted.get("pnrNumber").asText());
                if (extracted.hasNonNull("pnrConfidence")) result.setPnrConfidence(new BigDecimal(extracted.get("pnrConfidence").asText()));
                
                if (extracted.hasNonNull("passengerName")) result.setPassengerName(extracted.get("passengerName").asText());
                if (extracted.hasNonNull("passengerConfidence")) result.setPassengerConfidence(new BigDecimal(extracted.get("passengerConfidence").asText()));
                
                if (extracted.hasNonNull("travelDate")) {
                    try {
                        result.setTravelDate(LocalDate.parse(extracted.get("travelDate").asText(), DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception e) {
                        // ignore parsing error
                    }
                }
                if (extracted.hasNonNull("dateConfidence")) result.setDateConfidence(new BigDecimal(extracted.get("dateConfidence").asText()));
                
                if (extracted.hasNonNull("baseFare")) result.setBaseFare(new BigDecimal(extracted.get("baseFare").asText()));
                if (extracted.hasNonNull("amountConfidence")) result.setAmountConfidence(new BigDecimal(extracted.get("amountConfidence").asText()));
                
                if (extracted.hasNonNull("origin")) result.setOrigin(extracted.get("origin").asText());
                if (extracted.hasNonNull("destination")) result.setDestination(extracted.get("destination").asText());
                if (extracted.hasNonNull("operatorName")) result.setOperatorName(extracted.get("operatorName").asText());
                
                // Calculate average confidence
                BigDecimal totalConf = BigDecimal.ZERO;
                int fields = 0;
                
                if (result.getPnrConfidence() != null) { totalConf = totalConf.add(result.getPnrConfidence()); fields++; }
                if (result.getPassengerConfidence() != null) { totalConf = totalConf.add(result.getPassengerConfidence()); fields++; }
                if (result.getDateConfidence() != null) { totalConf = totalConf.add(result.getDateConfidence()); fields++; }
                if (result.getAmountConfidence() != null) { totalConf = totalConf.add(result.getAmountConfidence()); fields++; }
                
                if (fields > 0) {
                    result.setOverallConfidence(totalConf.divide(new BigDecimal(fields), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    result.setOverallConfidence(BigDecimal.ZERO);
                }
                
                return result;
            }
            
            // Fallback if parsing fails
            return regexExtractionService.fallbackExtraction(rawText, type);
            
        } catch (Exception e) {
            log.error("Failed to parse Gemini response JSON", e);
            return regexExtractionService.fallbackExtraction(rawText, type);
        }
    }
}
