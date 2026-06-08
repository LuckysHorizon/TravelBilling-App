package com.travelbillpro.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private boolean isProd() {
        return "prod".equalsIgnoreCase(activeProfile);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {} [{}]", ex.getMessage(), ex.getErrorCode());
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, ex.getHttpStatus());
    }

    @ExceptionHandler(ExtractionException.class)
    public ResponseEntity<ErrorResponse> handleExtractionException(ExtractionException ex) {
        log.warn("Extraction exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("EXTRACTION_FAILED", ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(PdfExtractionException.class)
    public ResponseEntity<ErrorResponse> handlePdfExtractionException(PdfExtractionException ex) {
        log.warn("PDF extraction exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("PDF_EXTRACTION_FAILED", ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NvidiaApiException.class)
    public ResponseEntity<ErrorResponse> handleNvidiaApiException(NvidiaApiException ex) {
        log.error("NVIDIA NIM API error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("AI_SERVICE_ERROR", "NVIDIA NIM error: " + ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);  // 502 for upstream service failure
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedRollback(UnexpectedRollbackException ex) {
        log.error("TRANSACTION ROLLBACK (this indicates a @Transactional boundary bug): {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse("TRANSACTION_ERROR",
            "Database transaction failed — check server logs for root cause", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse("INSUFFICIENT_ROLE", "You do not have permission to access this resource", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        ErrorResponse error = new ErrorResponse("UNAUTHORIZED", "Authentication failed", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File upload too large: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("FILE_TOO_LARGE",
            "File size exceeds maximum allowed upload size", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("MALFORMED_REQUEST",
            "Malformed JSON request body", LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", details);
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", details, LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        String message = isProd() ? "Data constraint violation" : ex.getMostSpecificCause().getMessage();
        ErrorResponse error = new ErrorResponse("DATA_CONFLICT", message, LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        String message = isProd() ? "An unexpected error occurred" : ex.getMessage();
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", message, LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ErrorResponse(String errorCode, String message, LocalDateTime timestamp) {}
}
