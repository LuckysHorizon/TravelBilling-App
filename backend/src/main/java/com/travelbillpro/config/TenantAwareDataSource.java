package com.travelbillpro.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * A DataSource wrapper that routes connections based on TenantContext.
 *
 * - If TenantContext has an orgId + dbUrl → route to the tenant's DB via TenantDataSourceManager
 * - Otherwise (SuperAdmin or no context) → route to the master DB
 *
 * This is registered as the PRIMARY DataSource bean, replacing Spring Boot's auto-configured one.
 */
@Slf4j
public class TenantAwareDataSource implements DataSource {

    private final DataSource masterDataSource;
    private final TenantDataSourceManager tenantDataSourceManager;

    public TenantAwareDataSource(DataSource masterDataSource, TenantDataSourceManager tenantDataSourceManager) {
        this.masterDataSource = masterDataSource;
        this.tenantDataSourceManager = tenantDataSourceManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource resolved = resolveDataSource();
        return resolved.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource resolved = resolveDataSource();
        return resolved.getConnection(username, password);
    }

    private DataSource resolveDataSource() {
        Long orgId = TenantContext.getOrgId();
        String dbUrl = TenantContext.getDbUrl();

        if (orgId != null && dbUrl != null && !dbUrl.isBlank()) {
            // Tenant request — route to tenant DB
            log.trace("Routing to tenant DB for org {}", orgId);
            return tenantDataSourceManager.getDataSource(orgId, dbUrl);
        }

        // SuperAdmin or unauthenticated — use master DB
        log.trace("Routing to master DB");
        return masterDataSource;
    }

    // --- Delegate all other DataSource methods to master ---

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return masterDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        masterDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        masterDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return masterDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return masterDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return masterDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || masterDataSource.isWrapperFor(iface);
    }
}
