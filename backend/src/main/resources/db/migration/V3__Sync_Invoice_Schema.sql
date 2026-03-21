-- V3__Sync_Invoice_Schema.sql
ALTER TABLE invoices ADD COLUMN invoice_date DATE;
ALTER TABLE invoices ADD COLUMN billing_period_start DATE;
ALTER TABLE invoices ADD COLUMN billing_period_end DATE;
ALTER TABLE invoices ADD COLUMN due_date DATE;

-- Update existing rows with some default values if necessary (or just leave NULL if no data yet)
UPDATE invoices SET invoice_date = created_at::DATE WHERE invoice_date IS NULL;
UPDATE invoices SET billing_period_start = created_at::DATE WHERE billing_period_start IS NULL;
UPDATE invoices SET billing_period_end = created_at::DATE WHERE billing_period_end IS NULL;
UPDATE invoices SET due_date = (created_at + INTERVAL '15 days')::DATE WHERE due_date IS NULL;

-- Make them NOT NULL after initial update if needed
ALTER TABLE invoices ALTER COLUMN invoice_date SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN billing_period_start SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN billing_period_end SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN due_date SET NOT NULL;
