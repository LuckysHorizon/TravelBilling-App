-- V4__Fix_Invoice_Sequences_And_Cleanup.sql
-- Fix column name mismatch: entity uses 'next_value' but V1 created 'next_number'
ALTER TABLE invoice_sequences RENAME COLUMN next_number TO next_value;
ALTER TABLE invoice_sequences ALTER COLUMN next_value TYPE BIGINT;
ALTER TABLE invoice_sequences ALTER COLUMN next_value SET DEFAULT 1;
