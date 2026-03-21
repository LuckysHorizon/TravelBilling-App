-- V8__Add_Extraction_Audits.sql
-- Stores every AI extraction call for audit, replay, and debugging

CREATE TABLE extraction_audits (
    id                BIGSERIAL PRIMARY KEY,
    source_filename   VARCHAR(255)   NOT NULL,
    pdf_structure     VARCHAR(30)    NOT NULL,
    ai_model_used     VARCHAR(100)   NOT NULL,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    raw_ai_response   TEXT,
    extraction_status VARCHAR(20)    NOT NULL DEFAULT 'STARTED',
    error_message     TEXT,
    processing_ms     BIGINT,
    ticket_count      INTEGER,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audits_filename ON extraction_audits(source_filename);
CREATE INDEX idx_audits_created  ON extraction_audits(created_at DESC);
CREATE INDEX idx_audits_status   ON extraction_audits(extraction_status);

-- Add extraction_method column to existing tickets table
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS extraction_method VARCHAR(30);
