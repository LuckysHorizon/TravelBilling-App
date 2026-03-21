package com.travelbillpro.service;

import com.travelbillpro.entity.ExtractionAudit;
import com.travelbillpro.repository.ExtractionAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes extraction audit records in their OWN independent transaction.
 *
 * Propagation.REQUIRES_NEW means: even if the caller has a rolled-back
 * or no transaction, this service ALWAYS opens a brand-new transaction,
 * commits it, and returns. It is completely isolated.
 *
 * This guarantees audit records are ALWAYS written — even when:
 * - AI extraction fails and throws exceptions
 * - Tickets fail to persist to the DB
 * - The parent transaction is rolled back
 *
 * CRITICAL: This service NEVER throws. All exceptions are caught and logged.
 * Audit failure must never mask the original pipeline error.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionAuditService {

    private final ExtractionAuditRepository auditRepository;

    /**
     * Always writes. Never throws. Never participates in caller's transaction.
     *
     * Propagation.REQUIRES_NEW: suspends any existing transaction,
     * opens a new one, commits/rolls back independently, then resumes original.
     *
     * @param context AuditContext accumulated throughout the extraction pipeline
     * @return The persisted audit record, or null if write failed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExtractionAudit writeAudit(AuditContext context) {
        try {
            if (context == null) {
                log.warn("Null AuditContext passed to writeAudit — skipping");
                return null;
            }

            ExtractionAudit audit = new ExtractionAudit();
            audit.setSourceFilename(context.getFilename());
            
            audit.setPdfStructure(context.getPdfStructure() != null ?
                    context.getPdfStructure() : "UNKNOWN");
            
            audit.setAiModelUsed(context.getModelUsed() != null ?
                    context.getModelUsed() : "meta/llama-3.1-70b-instruct");
            
            audit.setRawAiResponse(context.getRawAiResponse());
            audit.setPromptTokens(context.getPromptTokens());
            audit.setCompletionTokens(context.getCompletionTokens());
            audit.setExtractionStatus(context.getStatus());
            audit.setErrorMessage(context.getErrorMessage());
            audit.setProcessingMs(context.getProcessingMs());

            ExtractionAudit saved = auditRepository.save(audit);
            log.info("Audit written: id={}, status={}, file={}",
                    saved.getId(), saved.getExtractionStatus(), saved.getSourceFilename());
            return saved;

        } catch (Exception e) {
            // Must never propagate — audit failure cannot mask pipeline errors
            log.error("AUDIT WRITE FAILED (non-fatal): {}", e.getMessage(), e);
            return null;
        }
    }
}
