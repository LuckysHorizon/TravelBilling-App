-- V6: Widen billing_cycle column to accommodate 'BIWEEKLY' and future values
ALTER TABLE companies ALTER COLUMN billing_cycle TYPE VARCHAR(20);
