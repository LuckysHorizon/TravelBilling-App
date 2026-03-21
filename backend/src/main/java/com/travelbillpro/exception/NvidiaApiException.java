package com.travelbillpro.exception;

/**
 * Thrown when NVIDIA NIM API returns an error.
 * This is a RuntimeException — Spring will mark transaction as ROLLBACK-ONLY.
 * 
 * CRITICAL: This exception must NEVER be thrown inside a @Transactional method.
 * New design: AI service calls are NOT transactional; only DB writes are.
 */
public class NvidiaApiException extends RuntimeException {
    
    private final int httpStatusCode;
    
    public NvidiaApiException(String message) {
        super(message);
        this.httpStatusCode = 0;
    }
    
    public NvidiaApiException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }
    
    public NvidiaApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = 0;
    }
    
    public NvidiaApiException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }
    
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
