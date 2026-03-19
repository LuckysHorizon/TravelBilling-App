package com.travelbillpro.service;

import com.travelbillpro.dto.TicketExtractionResult;
import com.travelbillpro.enums.TicketType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RegexExtractionService {

    // Common patterns found in travel tickets
    private static final Pattern PNR_PATTERN = Pattern.compile("(?i)(pnr(?:\\s*no(?:umber)?|\\s*#)?[\\s:]*)([A-Z0-9]{6,8})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(?:date|travel\\s*date|departure)[\\s:]*(\\d{1,2}[-/\\s][A-Za-z]{3,8}[-/\\s]?\\d{2,4}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})");
    private static final Pattern FARE_PATTERN = Pattern.compile("(?i)(?:total|amount|fare|price)(?:\\s*paid|\\s*inr|\\s*rs)?(?:[\\s:.]+)*[₹]?\\s*(\\d{1,6}(?:[.,]\\d{2})?)");
    
    public TicketExtractionResult fallbackExtraction(String rawText, TicketType expectedType) {
        TicketExtractionResult result = new TicketExtractionResult();
        
        // PNR Extraction
        Matcher pnrMatcher = PNR_PATTERN.matcher(rawText);
        if (pnrMatcher.find()) {
            result.setPnrNumber(pnrMatcher.group(2));
            result.setPnrConfidence(new BigDecimal("60.00")); // Capped at 60 for regex fallback
        } else {
            result.setPnrConfidence(BigDecimal.ZERO);
        }

        // Date Extraction
        Matcher dateMatcher = DATE_PATTERN.matcher(rawText);
        if (dateMatcher.find()) {
            try {
                // Simplistic fallback date parsing. 
                // A real system would need robust multi-format parsing here.
                String dateStr = dateMatcher.group(1).replace("/", "-").trim();
                if (dateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    result.setTravelDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                    result.setDateConfidence(new BigDecimal("60.00"));
                }
            } catch (Exception e) {
                result.setDateConfidence(BigDecimal.ZERO);
            }
        }
        
        // Fare Extraction
        Matcher fareMatcher = FARE_PATTERN.matcher(rawText);
        if (fareMatcher.find()) {
            try {
                String amountStr = fareMatcher.group(1).replace(",", "");
                result.setBaseFare(new BigDecimal(amountStr));
                result.setAmountConfidence(new BigDecimal("60.00"));
            } catch (Exception e) {
                result.setAmountConfidence(BigDecimal.ZERO);
            }
        }
        
        // Passengers - hard to regex reliably
        result.setPassengerName("Not detected (Manual entry required)");
        result.setPassengerConfidence(BigDecimal.ZERO);
        
        // Operator - hard to regex reliably
        result.setOperatorName("Not detected");
        
        result.setTicketType(expectedType);
        
        // Calculate overall confidence (average of fields)
        BigDecimal totalConf = result.getPnrConfidence()
                .add(result.getDateConfidence() != null ? result.getDateConfidence() : BigDecimal.ZERO)
                .add(result.getAmountConfidence() != null ? result.getAmountConfidence() : BigDecimal.ZERO)
                .add(result.getPassengerConfidence() != null ? result.getPassengerConfidence() : BigDecimal.ZERO);
        
        BigDecimal overall = totalConf.divide(new BigDecimal("4"), 2, java.math.RoundingMode.HALF_UP);
        result.setOverallConfidence(overall);
        
        return result;
    }
}
