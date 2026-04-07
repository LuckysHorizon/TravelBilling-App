ALTER TABLE gst_config
ADD COLUMN IF NOT EXISTS service_charge_per_ticket DECIMAL(10,2) DEFAULT 0;