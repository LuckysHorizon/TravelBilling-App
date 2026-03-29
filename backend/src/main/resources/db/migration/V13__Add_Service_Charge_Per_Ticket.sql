-- Add service_charge_per_ticket column to gst_config for flat ₹ billing per ticket
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE gst_config ADD service_charge_per_ticket DECIMAL(10,2) DEFAULT 0';
EXCEPTION
  WHEN OTHERS THEN
	IF SQLCODE != -1430 THEN
	  RAISE;
	END IF;
END;
/
