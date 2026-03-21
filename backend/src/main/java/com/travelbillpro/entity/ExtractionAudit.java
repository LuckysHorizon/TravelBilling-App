package com.travelbillpro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "extraction_audits")
@Getter
@Setter
public class ExtractionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_filename", nullable = false)
    private String sourceFilename;

    @Column(name = "pdf_structure", nullable = false, length = 30)
    private String pdfStructure;

    @Column(name = "ai_model_used", nullable = false, length = 100)
    private String aiModelUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Column(name = "extraction_status", nullable = false, length = 20)
    private String extractionStatus = "STARTED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_ms")
    private Long processingMs;

    @Column(name = "ticket_count")
    private Integer ticketCount;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
