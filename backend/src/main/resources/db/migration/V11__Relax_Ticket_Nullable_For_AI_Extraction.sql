-- V11: Relax NOT NULL constraints on ticket columns for AI extraction compatibility
-- Service charge, CGST, SGST are calculated at approval time, not during extraction.
-- PNR, travel_date, base_fare, total_amount may be partial from AI extraction.

ALTER TABLE tickets ALTER COLUMN pnr_number DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN passenger_name DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN travel_date DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN base_fare DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN service_charge DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN cgst DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN sgst DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN total_amount DROP NOT NULL;
