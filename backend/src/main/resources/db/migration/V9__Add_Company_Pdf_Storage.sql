-- Add custom PDF storage path to companies table
ALTER TABLE companies ADD COLUMN IF NOT EXISTS pdf_storage_path TEXT;
