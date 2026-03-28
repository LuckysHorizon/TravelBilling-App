-- Add service_charge_per_ticket column to gst_config for flat ₹ billing per ticket
ALTER TABLE gst_config ADD COLUMN IF NOT EXISTS service_charge_per_ticket DECIMAL(10,2) DEFAULT 0;
