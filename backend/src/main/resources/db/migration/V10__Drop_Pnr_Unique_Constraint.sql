-- V10: Drop unique constraint on pnr_number to allow group bookings
-- Group booking PDFs (e.g. IndiGo) produce multiple passengers sharing the same PNR.
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_pnr_number_key;

-- Also drop any unique index that might exist
DROP INDEX IF EXISTS tickets_pnr_number_key;
