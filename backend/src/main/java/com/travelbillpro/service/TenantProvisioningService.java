package com.travelbillpro.service;

import com.travelbillpro.config.TenantDataSourceManager;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Provisions a new tenant database by running the full schema SQL
 * against a Supabase (or any PostgreSQL) URL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final TenantDataSourceManager dataSourceManager;
    private final PasswordEncoder passwordEncoder;

    /**
     * Provision a new tenant database:
     * 1. Test connection
     * 2. Create all schema tables
     * 3. Seed admin user + defaults
     *
     * @return provisioning log
     */
    public String provision(String dbUrl, String adminUsername, String adminPassword, String adminEmail) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Starting provisioning...\n");

        try (HikariDataSource ds = dataSourceManager.createProvisioningDataSource(dbUrl)) {
            try (Connection conn = ds.getConnection()) {
                logBuilder.append("✓ Connected to database\n");

                try (Statement stmt = conn.createStatement()) {
                    // Step 1: Create schema (empty tables only — NO data from master DB)
                    logBuilder.append("Creating tables...\n");
                    stmt.execute(getSchemaSQL());
                    logBuilder.append("✓ All tables created (empty — no data copied)\n");

                    // Step 2: Create ONLY the org admin user
                    logBuilder.append("Creating org admin user...\n");
                    String hashedPassword = passwordEncoder.encode(adminPassword);
                    String seedSQL = getSeedSQL(adminUsername, adminEmail, hashedPassword);
                    stmt.execute(seedSQL);
                    logBuilder.append("✓ Org admin user created: " + adminUsername + "\n");

                    // NOTE: No default config is seeded.
                    // The org admin must configure GST rates, agency details, etc. after first login.

                    logBuilder.append("✅ Provisioning complete! (clean DB — no master data copied)\n");
                }
            }
        } catch (Exception e) {
            log.error("Provisioning failed", e);
            logBuilder.append("❌ ERROR: ").append(e.getMessage()).append("\n");
            throw new RuntimeException("Provisioning failed: " + e.getMessage(), e);
        }

        return logBuilder.toString();
    }

    /**
     * Drop ALL tables in the tenant database (for org deletion).
     */
    public void dropAllTables(String dbUrl) {
        try (HikariDataSource ds = dataSourceManager.createProvisioningDataSource(dbUrl)) {
            try (Connection conn = ds.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    // Get all table names in public schema
                    var rs = stmt.executeQuery(
                        "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
                    );
                    java.util.List<String> tables = new java.util.ArrayList<>();
                    while (rs.next()) {
                        tables.add(rs.getString("tablename"));
                    }
                    rs.close();

                    if (!tables.isEmpty()) {
                        // DROP CASCADE all tables at once
                        String dropSQL = "DROP TABLE IF EXISTS " +
                            String.join(", ", tables.stream()
                                .map(t -> "\"" + t + "\"")
                                .toArray(String[]::new)) +
                            " CASCADE";
                        stmt.execute(dropSQL);
                        log.info("Dropped {} tables from tenant DB", tables.size());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to drop tables in tenant DB", e);
            throw new RuntimeException("Failed to drop tables: " + e.getMessage(), e);
        }
    }

    /**
     * Combined schema SQL from V1 through V11 — creates all tables from scratch.
     */
    private String getSchemaSQL() {
        return """
            -- Users
            CREATE TABLE IF NOT EXISTS users (
                id BIGSERIAL PRIMARY KEY,
                username VARCHAR(100) UNIQUE NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL,
                failed_attempts INTEGER DEFAULT 0,
                locked_until TIMESTAMP,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            -- Companies
            CREATE TABLE IF NOT EXISTS companies (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                gst_number VARCHAR(15),
                billing_email VARCHAR(255),
                address TEXT,
                service_charge_pct DECIMAL(5,2) DEFAULT 0,
                billing_cycle VARCHAR(20) DEFAULT 'MONTHLY',
                credit_limit DECIMAL(12,2),
                is_active BOOLEAN DEFAULT TRUE,
                contact_name VARCHAR(255),
                phone VARCHAR(20),
                city VARCHAR(100),
                state VARCHAR(100),
                pin_code VARCHAR(10),
                pdf_storage_path TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by BIGINT REFERENCES users(id)
            );

            -- Invoice Sequences
            CREATE TABLE IF NOT EXISTS invoice_sequences (
                financial_year VARCHAR(7) PRIMARY KEY,
                next_value BIGINT DEFAULT 1
            );

            -- Invoices
            CREATE TABLE IF NOT EXISTS invoices (
                id BIGSERIAL PRIMARY KEY,
                invoice_number VARCHAR(30) UNIQUE NOT NULL,
                company_id BIGINT NOT NULL REFERENCES companies(id),
                billing_month VARCHAR(7),
                invoice_date DATE NOT NULL DEFAULT CURRENT_DATE,
                billing_period_start DATE NOT NULL DEFAULT CURRENT_DATE,
                billing_period_end DATE NOT NULL DEFAULT CURRENT_DATE,
                due_date DATE NOT NULL DEFAULT CURRENT_DATE,
                subtotal DECIMAL(12,2) DEFAULT 0,
                service_charge DECIMAL(12,2) DEFAULT 0,
                cgst_total DECIMAL(12,2) DEFAULT 0,
                sgst_total DECIMAL(12,2) DEFAULT 0,
                grand_total DECIMAL(12,2) DEFAULT 0,
                status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                excel_s3_key TEXT,
                pdf_s3_key TEXT,
                email_sent_at TIMESTAMP,
                paid_at TIMESTAMP,
                employee_id BIGINT,
                cgst_rate DECIMAL(5,2),
                sgst_rate DECIMAL(5,2),
                total_in_words TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by BIGINT REFERENCES users(id)
            );

            -- Tickets
            CREATE TABLE IF NOT EXISTS tickets (
                id BIGSERIAL PRIMARY KEY,
                company_id BIGINT NOT NULL REFERENCES companies(id),
                pnr_number VARCHAR(20),
                ticket_type VARCHAR(10) NOT NULL DEFAULT 'FLIGHT',
                passenger_name VARCHAR(255),
                travel_date DATE,
                origin VARCHAR(100),
                destination VARCHAR(100),
                operator_name VARCHAR(150),
                base_fare DECIMAL(10,2),
                service_charge DECIMAL(10,2),
                cgst DECIMAL(10,2),
                sgst DECIMAL(10,2),
                total_amount DECIMAL(10,2),
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                file_path TEXT,
                ai_confidence DECIMAL(5,2),
                extraction_method VARCHAR(30),
                invoice_id BIGINT REFERENCES invoices(id),
                employee_id BIGINT,
                passenger_service_fee DECIMAL(10,2),
                user_development_charges DECIMAL(10,2),
                agent_service_charges DECIMAL(10,2),
                other_charges DECIMAL(10,2),
                discount DECIMAL(10,2),
                sac_code_air VARCHAR(20),
                sac_code_agent VARCHAR(20),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by BIGINT REFERENCES users(id)
            );

            -- Audit Logs
            CREATE TABLE IF NOT EXISTS audit_logs (
                id BIGSERIAL PRIMARY KEY,
                entity_type VARCHAR(50) NOT NULL,
                entity_id BIGINT NOT NULL,
                action VARCHAR(50) NOT NULL,
                old_value JSONB,
                new_value JSONB,
                user_id BIGINT REFERENCES users(id),
                ip_address VARCHAR(45),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            -- GST Config
            CREATE TABLE IF NOT EXISTS gst_config (
                id BIGSERIAL PRIMARY KEY,
                cgst_rate DECIMAL(5,2) NOT NULL,
                sgst_rate DECIMAL(5,2) NOT NULL,
                effective_from DATE NOT NULL,
                created_by BIGINT REFERENCES users(id),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            -- Credit Notes
            CREATE TABLE IF NOT EXISTS credit_notes (
                id BIGSERIAL PRIMARY KEY,
                credit_note_number VARCHAR(30) UNIQUE NOT NULL,
                invoice_id BIGINT NOT NULL REFERENCES invoices(id),
                reason TEXT NOT NULL,
                amount DECIMAL(12,2) NOT NULL,
                type VARCHAR(10) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by BIGINT REFERENCES users(id)
            );

            -- Email Templates
            CREATE TABLE IF NOT EXISTS email_templates (
                key VARCHAR(50) PRIMARY KEY,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_by BIGINT REFERENCES users(id)
            );

            -- System Config
            CREATE TABLE IF NOT EXISTS system_config (
                key VARCHAR(50) PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_by BIGINT REFERENCES users(id)
            );

            -- Billing Panels
            CREATE TABLE IF NOT EXISTS billing_panels (
                id BIGSERIAL PRIMARY KEY,
                label VARCHAR(255) NOT NULL,
                description TEXT,
                billing_month VARCHAR(7),
                status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                total_amount DECIMAL(12,2) DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by BIGINT REFERENCES users(id)
            );

            -- Extraction Audits
            CREATE TABLE IF NOT EXISTS extraction_audits (
                id BIGSERIAL PRIMARY KEY,
                source_filename VARCHAR(255) NOT NULL,
                pdf_structure VARCHAR(30) NOT NULL,
                ai_model_used VARCHAR(100) NOT NULL,
                prompt_tokens INTEGER,
                completion_tokens INTEGER,
                raw_ai_response TEXT,
                extraction_status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
                error_message TEXT,
                processing_ms BIGINT,
                ticket_count INTEGER,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            -- Employees
            CREATE TABLE IF NOT EXISTS employees (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                mobile VARCHAR(20),
                company_id BIGINT REFERENCES companies(id),
                is_frequent_flyer BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            -- Invoice Line Items
            CREATE TABLE IF NOT EXISTS invoice_line_items (
                id BIGSERIAL PRIMARY KEY,
                invoice_id BIGINT NOT NULL REFERENCES invoices(id),
                ticket_id BIGINT REFERENCES tickets(id),
                particulars VARCHAR(500),
                sac_code VARCHAR(20),
                taxable_value DECIMAL(12,2) DEFAULT 0,
                non_taxable_value DECIMAL(12,2) DEFAULT 0,
                total DECIMAL(12,2) DEFAULT 0,
                is_manually_added BOOLEAN DEFAULT FALSE
            );

            -- Indexes
            CREATE INDEX IF NOT EXISTS idx_tickets_company_status ON tickets(company_id, status);
            CREATE INDEX IF NOT EXISTS idx_tickets_travel_date ON tickets(travel_date);
            CREATE INDEX IF NOT EXISTS idx_tickets_invoice_id ON tickets(invoice_id);
            CREATE INDEX IF NOT EXISTS idx_invoices_company_month ON invoices(company_id, billing_month);
            CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
            CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id, created_at);
            CREATE INDEX IF NOT EXISTS idx_companies_active ON companies(name) WHERE is_active = true;
            CREATE INDEX IF NOT EXISTS idx_audits_filename ON extraction_audits(source_filename);
            CREATE INDEX IF NOT EXISTS idx_audits_created ON extraction_audits(created_at DESC);
            CREATE INDEX IF NOT EXISTS idx_audits_status ON extraction_audits(extraction_status);
            """;
    }

    /**
     * Create org admin user SQL.
     */
    private String getSeedSQL(String username, String email, String hashedPassword) {
        return String.format("""
            INSERT INTO users (username, email, password_hash, role, created_at, updated_at)
            VALUES ('%s', '%s', '%s', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (username) DO NOTHING;
            """,
            username.replace("'", "''"),
            email.replace("'", "''"),
            hashedPassword.replace("'", "''")
        );
    }

    /**
     * Seed default config data.
     */
    private String getDefaultConfigSQL() {
        return """
            INSERT INTO gst_config (cgst_rate, sgst_rate, effective_from, created_by, created_at)
            VALUES (9.00, 9.00, '2024-04-01',
                    (SELECT id FROM users LIMIT 1),
                    CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING;

            INSERT INTO system_config (key, value, updated_at, updated_by) VALUES
            ('AGENCY_NAME', 'My Travel Agency', CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1)),
            ('AGENCY_GSTIN', '00XXXXX0000X0Z0', CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1)),
            ('AGENCY_ADDRESS', 'Address not configured', CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1)),
            ('INVOICE_PREFIX', 'INV', CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1))
            ON CONFLICT (key) DO NOTHING;

            INSERT INTO email_templates (key, subject, body, updated_at, updated_by) VALUES
            ('invoice_sent', 'Invoice {invoiceNumber} - {companyName} - {billingMonth}',
             '<p>Dear {companyName},</p><p>Please find attached invoice <strong>{invoiceNumber}</strong>.</p>',
             CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1)),
            ('payment_reminder', 'Payment Due - Invoice {invoiceNumber}',
             '<p>Dear {companyName},</p><p>Reminder: invoice <strong>{invoiceNumber}</strong> for <strong>{grandTotal}</strong> is overdue.</p>',
             CURRENT_TIMESTAMP, (SELECT id FROM users LIMIT 1))
            ON CONFLICT (key) DO NOTHING;
            """;
    }
}
