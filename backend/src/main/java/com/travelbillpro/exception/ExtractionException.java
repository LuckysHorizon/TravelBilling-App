package com.travelbillpro.exception;

/**
 * Thrown when PDF or AI extraction fails with known domain errors.
 * This is a RuntimeException — Spring will mark transaction as ROLLBACK-ONLY.
 * 
 * CRITICAL: This exception must NEVER be thrown inside a @Transactional method.
 * New design: extraction methods are NOT transactional; only DB writes are.
 */
public class ExtractionException extends RuntimeException {
    
    public ExtractionException(String message) {
        super(message);
    }
    
    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
