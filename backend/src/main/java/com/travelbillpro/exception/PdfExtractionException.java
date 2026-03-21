package com.travelbillpro.exception;

/**
 * Thrown when PDF parsing or extraction fails (file I/O, malformed PDF, etc).
 * This is a RuntimeException to satisfy Spring's @Transactional conventions.
 */
public class PdfExtractionException extends RuntimeException {
    
    public PdfExtractionException(String message) {
        super(message);
    }
    
    public PdfExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
