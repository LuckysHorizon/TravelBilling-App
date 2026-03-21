-- Support TRAIN and UNKNOWN ticket types
-- The column is VARCHAR(10) which is already wide enough for UNKNOWN (7 chars).
-- No ALTER needed — JPA @Enumerated(EnumType.STRING) stores enum name directly.
-- This migration is a no-op placeholder to document the schema compatibility.
SELECT 1;
