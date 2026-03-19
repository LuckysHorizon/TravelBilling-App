-- V1__Initial_Schema.sql

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    failed_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    gst_number VARCHAR(15) UNIQUE NOT NULL,
    billing_email VARCHAR(255) NOT NULL,
    address TEXT,
    service_charge_pct DECIMAL(5,2) NOT NULL,
    billing_cycle VARCHAR(10) NOT NULL,
    credit_limit DECIMAL(12,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(30) UNIQUE NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    billing_month VARCHAR(7) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    service_charge DECIMAL(12,2) NOT NULL,
    cgst_total DECIMAL(12,2) NOT NULL,
    sgst_total DECIMAL(12,2) NOT NULL,
    grand_total DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    excel_s3_key TEXT,
    pdf_s3_key TEXT,
    email_sent_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    pnr_number VARCHAR(20) UNIQUE NOT NULL,
    ticket_type VARCHAR(10) NOT NULL,
    passenger_name VARCHAR(255) NOT NULL,
    travel_date DATE NOT NULL,
    origin VARCHAR(100),
    destination VARCHAR(100),
    operator_name VARCHAR(150),
    base_fare DECIMAL(10,2) NOT NULL,
    service_charge DECIMAL(10,2) NOT NULL,
    cgst DECIMAL(10,2) NOT NULL,
    sgst DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    file_path TEXT,
    ai_confidence DECIMAL(5,2),
    invoice_id BIGINT REFERENCES invoices(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    user_id BIGINT REFERENCES users(id),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE gst_config (
    id BIGSERIAL PRIMARY KEY,
    cgst_rate DECIMAL(5,2) NOT NULL,
    sgst_rate DECIMAL(5,2) NOT NULL,
    effective_from DATE NOT NULL,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE credit_notes (
    id BIGSERIAL PRIMARY KEY,
    credit_note_number VARCHAR(30) UNIQUE NOT NULL,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    reason TEXT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);

CREATE TABLE email_templates (
    key VARCHAR(50) PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES users(id)
);

CREATE TABLE system_config (
    key VARCHAR(50) PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES users(id)
);

CREATE TABLE invoice_sequences (
    financial_year VARCHAR(7) PRIMARY KEY,
    next_number INTEGER DEFAULT 1
);

-- Indexes
CREATE INDEX idx_tickets_company_status ON tickets(company_id, status);
CREATE INDEX idx_tickets_travel_date ON tickets(travel_date);
CREATE INDEX idx_tickets_invoice_id ON tickets(invoice_id);
CREATE INDEX idx_invoices_company_month ON invoices(company_id, billing_month);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at);
CREATE INDEX idx_companies_active ON companies(name) WHERE is_active = true;
