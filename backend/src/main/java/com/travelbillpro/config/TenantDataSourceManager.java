package com.travelbillpro.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tenant DataSource instances — one HikariCP pool per org.
 * Creates pools lazily on first access and caches them.
 */
@Component
@Slf4j
public class TenantDataSourceManager {

    private final Map<Long, HikariDataSource> tenantDataSources = new ConcurrentHashMap<>();

    /**
     * Get or create a DataSource for the given org.
     * On first creation, runs idempotent schema repair to add any missing columns.
     */
    public DataSource getDataSource(Long orgId, String dbUrl) {
        return tenantDataSources.computeIfAbsent(orgId, id -> {
            log.info("Creating DataSource pool for org {}", id);
            HikariDataSource ds = createDataSource(dbUrl, "tenant-" + id);
            runSchemaRepair(ds, id);
            return ds;
        });
    }

    /**
     * Auto-repair: add missing columns to existing tenant tables.
     * Idempotent — safe to run every time a DS is created.
     */
    private void runSchemaRepair(HikariDataSource ds, Long orgId) {
        String repairSQL = """
            -- tickets table
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS billing_panel_id BIGINT;
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS agent_service_charges DECIMAL(10,2);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS other_charges DECIMAL(10,2);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS discount DECIMAL(10,2);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS passenger_service_fee DECIMAL(10,2);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS user_development_charges DECIMAL(10,2);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS sac_code_air VARCHAR(20);
            ALTER TABLE tickets ADD COLUMN IF NOT EXISTS sac_code_agent VARCHAR(20);
            -- billing_panels table
            ALTER TABLE billing_panels ADD COLUMN IF NOT EXISTS company_id BIGINT;
            ALTER TABLE billing_panels ADD COLUMN IF NOT EXISTS invoice_id BIGINT;
            -- gst_config table
            ALTER TABLE gst_config ADD COLUMN IF NOT EXISTS service_charge_per_ticket DECIMAL(10,2) DEFAULT 0;
            -- users table
            ALTER TABLE users ADD COLUMN IF NOT EXISTS org_id BIGINT;
            """;
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(repairSQL);
            log.info("✓ Schema repair completed for org {}", orgId);
        } catch (Exception e) {
            log.warn("Schema repair skipped for org {} (tables may not exist yet): {}", orgId, e.getMessage());
        }
    }

    /**
     * Create a one-off DataSource for provisioning (not cached).
     */
    public HikariDataSource createProvisioningDataSource(String dbUrl) {
        return createDataSource(dbUrl, "provisioning-" + System.currentTimeMillis());
    }

    /**
     * Test if a DB URL is reachable. Returns a map with "success" and "message".
     */
    public Map<String, Object> testConnection(String dbUrl) {
        HikariDataSource ds = null;
        try {
            ds = createDataSource(dbUrl, "test-" + System.currentTimeMillis());
            try (Connection conn = ds.getConnection()) {
                // Run a real query to confirm everything works
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    if (rs.next()) {
                        log.info("DB connection test SUCCESSFUL for host");
                        return Map.of("success", true, "message", "Connected successfully");
                    }
                }
                return Map.of("success", false, "message", "Query returned no result");
            }
        } catch (Exception e) {
            String rootCause = getRootCauseMessage(e);
            log.error("DB connection test FAILED: {}", rootCause, e);
            return Map.of("success", false, "message", rootCause);
        } finally {
            if (ds != null && !ds.isClosed()) {
                try { ds.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Remove a cached DataSource (e.g., when org is suspended).
     */
    public void evict(Long orgId) {
        HikariDataSource removed = tenantDataSources.remove(orgId);
        if (removed != null && !removed.isClosed()) {
            removed.close();
            log.info("Evicted DataSource pool for org {}", orgId);
        }
    }

    private String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = root.getClass().getSimpleName();
        }
        return msg;
    }

    private HikariDataSource createDataSource(String dbUrl, String poolName) {
        // Strip whitespace/newlines from URL (copy-paste artifact)
        dbUrl = dbUrl.replaceAll("\\s+", "");

        HikariConfig config = new HikariConfig();

        // Parse Supabase/PostgreSQL URI format: postgresql://user:pass@host:port/dbname
        if (dbUrl.startsWith("postgresql://") || dbUrl.startsWith("postgres://")) {
            try {
                // Replace scheme to use java.net.URI parser
                String httpUrl = dbUrl.replaceFirst("postgres(ql)?://", "http://");
                java.net.URI uri = new java.net.URI(httpUrl);

                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    config.setUsername(parts[0]);
                    if (parts.length > 1) config.setPassword(parts[1]);
                }

                // Rebuild clean JDBC URL without credentials
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "/postgres";
                StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                        .append(uri.getHost()).append(":").append(port).append(path);

                // Add SSL for non-localhost (Supabase requires it)
                boolean isRemote = !uri.getHost().equals("localhost") && !uri.getHost().equals("127.0.0.1");
                if (uri.getQuery() != null) {
                    jdbc.append("?").append(uri.getQuery());
                    if (isRemote && !uri.getQuery().contains("sslmode")) {
                        jdbc.append("&sslmode=require");
                    }
                } else if (isRemote) {
                    jdbc.append("?sslmode=require");
                }

                String jdbcUrl = jdbc.toString();
                config.setJdbcUrl(jdbcUrl);
                log.info("Parsed DB URL → jdbc={}", jdbcUrl.replaceAll("password=[^&]*", "password=***"));
            } catch (Exception e) {
                log.warn("Failed to parse DB URL, using as-is: {}", e.getMessage());
                String jdbc = dbUrl.replace("postgresql://", "jdbc:postgresql://")
                                   .replace("postgres://", "jdbc:postgresql://");
                config.setJdbcUrl(jdbc);
            }
        } else {
            // Already a JDBC URL or other format
            config.setJdbcUrl(dbUrl);
        }

        config.setPoolName(poolName);
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(30000); // 30s for remote DBs
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }
}
