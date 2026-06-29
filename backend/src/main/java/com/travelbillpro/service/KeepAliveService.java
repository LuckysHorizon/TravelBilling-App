package com.travelbillpro.service;

import com.travelbillpro.config.TenantContext;
import com.travelbillpro.config.TenantDataSourceManager;
import com.travelbillpro.config.TenantDataSourceManager.JdbcUrlInfo;
import com.travelbillpro.dto.KeepAliveDto.*;
import com.travelbillpro.entity.DatabaseHealthCheck;
import com.travelbillpro.entity.Organization;
import com.travelbillpro.repository.DatabaseHealthCheckRepository;
import com.travelbillpro.repository.OrganizationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Core keep-alive service. Pings the super-admin DB and all active tenant DBs,
 * records health results, and provides status queries.
 */
@Service
@Slf4j
public class KeepAliveService {

    private static final String SUPER_ADMIN_DB_ID = "super-admin";

    private final DatabaseHealthCheckRepository healthCheckRepo;
    private final OrganizationRepository orgRepo;
    private final ExecutorService keepAliveExecutor;

    @Value("${spring.datasource.url}")
    private String masterDbUrl;

    @Value("${spring.datasource.username:}")
    private String masterDbUsername;

    @Value("${spring.datasource.password:}")
    private String masterDbPassword;

    @Value("${app.keepalive.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${app.keepalive.retention-days:90}")
    private int retentionDays;

    public KeepAliveService(
            DatabaseHealthCheckRepository healthCheckRepo,
            OrganizationRepository orgRepo,
            @Qualifier("keepAliveExecutor") ExecutorService keepAliveExecutor) {
        this.healthCheckRepo = healthCheckRepo;
        this.orgRepo = orgRepo;
        this.keepAliveExecutor = keepAliveExecutor;
    }

    /**
     * Execute a full keep-alive run: ping super-admin + all active org databases.
     * Each ping is independent — one failure does not stop others.
     */
    public KeepAliveResult executeKeepAlive() {
        UUID executionId = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.now();
        log.info("[KEEPALIVE] Starting keep-alive execution (id={})", executionId);

        List<DatabasePingResult> results = new ArrayList<>();

        // ── 1. Ping super-admin database ────────────────────────
        log.info("[KEEPALIVE] Pinging Super Admin database...");
        DatabasePingResult superAdminResult = pingSuperAdminDatabase(executionId);
        results.add(superAdminResult);
        logPingResult("Super Admin", superAdminResult);

        // ── 2. Fetch all active organizations ───────────────────
        // Ensure we're using the master DB context for org queries
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        List<Organization> activeOrgs;
        try {
            TenantContext.clear();
            activeOrgs = orgRepo.findByStatus("ACTIVE");
        } finally {
            // Restore context if it was set (e.g., called from admin endpoint)
            if (savedOrgId != null) {
                TenantContext.setOrgId(savedOrgId);
                TenantContext.setDbUrl(savedDbUrl);
                TenantContext.setOrgSlug(savedOrgSlug);
            }
        }

        log.info("[KEEPALIVE] Found {} active organizations to ping", activeOrgs.size());

        // ── 3. Ping all org databases in parallel ───────────────
        if (!activeOrgs.isEmpty()) {
            List<CompletableFuture<DatabasePingResult>> futures = activeOrgs.stream()
                    .map(org -> CompletableFuture.supplyAsync(
                            () -> pingOrgDatabase(org, executionId),
                            keepAliveExecutor
                    ))
                    .toList();

            // Wait for all pings to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<DatabasePingResult> future : futures) {
                try {
                    DatabasePingResult result = future.get();
                    results.add(result);
                    logPingResult(result.orgName(), result);
                } catch (Exception e) {
                    log.error("[KEEPALIVE] Unexpected error collecting ping result", e);
                }
            }
        }

        // ── 4. Build summary ────────────────────────────────────
        int successes = (int) results.stream().filter(DatabasePingResult::success).count();
        int failures = results.size() - successes;
        long avgResponseTime = results.stream()
                .filter(r -> r.responseTimeMs() != null)
                .mapToInt(DatabasePingResult::responseTimeMs)
                .average()
                .stream().mapToLong(d -> (long) d)
                .findFirst().orElse(0L);

        log.info("[KEEPALIVE] Completed: {}/{} success, avg {}ms (id={})",
                successes, results.size(), avgResponseTime, executionId);

        return new KeepAliveResult(
                executionId, results.size(), successes, failures,
                avgResponseTime, startTime, results
        );
    }

    /**
     * Get the status of the most recent keep-alive execution + per-DB health.
     */
    public KeepAliveStatusResponse getStatus() {
        // Ensure master DB context
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        try {
            TenantContext.clear();
            return buildStatusResponse();
        } finally {
            if (savedOrgId != null) {
                TenantContext.setOrgId(savedOrgId);
                TenantContext.setDbUrl(savedDbUrl);
                TenantContext.setOrgSlug(savedOrgSlug);
            }
        }
    }

    /**
     * Delete health check records older than the configured retention period.
     */
    @Transactional
    public int cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = healthCheckRepo.deleteByCheckedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("[KEEPALIVE] Cleaned up {} health check records older than {} days", deleted, retentionDays);
        }
        return deleted;
    }

    // ═══════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════

    private DatabasePingResult pingSuperAdminDatabase(UUID executionId) {
        long start = System.nanoTime();
        try (Connection conn = DriverManager.getConnection(masterDbUrl, masterDbUsername, masterDbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            int elapsed = (int) ((System.nanoTime() - start) / 1_000_000);

            DatabaseHealthCheck check = buildCheck(SUPER_ADMIN_DB_ID, null, "Super Admin", null,
                    true, elapsed, null, executionId);
            healthCheckRepo.save(check);

            return new DatabasePingResult(SUPER_ADMIN_DB_ID, "Super Admin", true, elapsed, null);

        } catch (Exception e) {
            int elapsed = (int) ((System.nanoTime() - start) / 1_000_000);
            String errorMsg = getRootCauseMessage(e);

            DatabaseHealthCheck check = buildCheck(SUPER_ADMIN_DB_ID, null, "Super Admin", null,
                    false, elapsed, errorMsg, executionId);
            healthCheckRepo.save(check);

            return new DatabasePingResult(SUPER_ADMIN_DB_ID, "Super Admin", false, elapsed, errorMsg);
        }
    }

    private DatabasePingResult pingOrgDatabase(Organization org, UUID executionId) {
        String databaseId = "org-" + org.getId();
        String dbUrl = org.getDbUrl();
        log.info("[KEEPALIVE] Pinging org: {} ({})", org.getName(), databaseId);

        long start = System.nanoTime();
        Connection conn = null;
        try {
            // Parse the Supabase URL using the shared parser
            JdbcUrlInfo urlInfo = TenantDataSourceManager.parseDbUrl(dbUrl);

            // Set connection timeout via DriverManager property
            Properties props = new Properties();
            if (urlInfo.username() != null) props.setProperty("user", urlInfo.username());
            if (urlInfo.password() != null) props.setProperty("password", urlInfo.password());
            props.setProperty("loginTimeout", String.valueOf(connectionTimeoutMs / 1000));
            props.setProperty("connectTimeout", String.valueOf(connectionTimeoutMs / 1000));
            props.setProperty("socketTimeout", String.valueOf(connectionTimeoutMs / 1000));

            // Set DriverManager-level timeout (seconds)
            DriverManager.setLoginTimeout(connectionTimeoutMs / 1000);

            conn = DriverManager.getConnection(urlInfo.jdbcUrl(), props);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                // Consume the result to confirm the connection is alive
                rs.next();
            }

            int elapsed = (int) ((System.nanoTime() - start) / 1_000_000);

            DatabaseHealthCheck check = buildCheck(databaseId, org.getId(), org.getName(),
                    org.getSlug(), true, elapsed, null, executionId);
            saveCheckInMasterContext(check);

            return new DatabasePingResult(databaseId, org.getName(), true, elapsed, null);

        } catch (Exception e) {
            int elapsed = (int) ((System.nanoTime() - start) / 1_000_000);
            String errorMsg = getRootCauseMessage(e);

            DatabaseHealthCheck check = buildCheck(databaseId, org.getId(), org.getName(),
                    org.getSlug(), false, elapsed, errorMsg, executionId);
            saveCheckInMasterContext(check);

            return new DatabasePingResult(databaseId, org.getName(), false, elapsed, errorMsg);

        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Save a health check record using the master DB context.
     * Since ping threads run outside any tenant context, we ensure the
     * save always hits the master database where the health_checks table lives.
     */
    private void saveCheckInMasterContext(DatabaseHealthCheck check) {
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        try {
            TenantContext.clear();
            healthCheckRepo.save(check);
        } finally {
            if (savedOrgId != null) {
                TenantContext.setOrgId(savedOrgId);
                TenantContext.setDbUrl(savedDbUrl);
                TenantContext.setOrgSlug(savedOrgSlug);
            }
        }
    }

    private DatabaseHealthCheck buildCheck(String databaseId, Long orgId, String orgName,
                                            String orgSlug, boolean success, Integer responseTimeMs,
                                            String errorMessage, UUID executionId) {
        DatabaseHealthCheck check = new DatabaseHealthCheck();
        check.setDatabaseId(databaseId);
        check.setOrgId(orgId);
        check.setOrgName(orgName);
        check.setOrgSlug(orgSlug);
        check.setSuccess(success);
        check.setResponseTimeMs(responseTimeMs);
        check.setErrorMessage(errorMessage);
        check.setExecutionId(executionId);
        return check;
    }

    private KeepAliveStatusResponse buildStatusResponse() {
        // Get latest execution
        List<DatabaseHealthCheck> latestRun = healthCheckRepo.findLatestExecution();
        if (latestRun.isEmpty()) {
            return new KeepAliveStatusResponse(null, null, 0, 0, 0, 0, List.of());
        }

        DatabaseHealthCheck first = latestRun.get(0);
        UUID lastExecutionId = first.getExecutionId();
        LocalDateTime lastExecutionTime = first.getCheckedAt();

        int successes = (int) latestRun.stream().filter(DatabaseHealthCheck::isSuccess).count();
        int failures = latestRun.size() - successes;
        long avgTime = (long) latestRun.stream()
                .filter(h -> h.getResponseTimeMs() != null)
                .mapToInt(DatabaseHealthCheck::getResponseTimeMs)
                .average().orElse(0);

        // Per-database status
        List<String> dbIds = healthCheckRepo.findAllDistinctDatabaseIds();
        List<DatabaseStatus> dbStatuses = dbIds.stream().map(dbId -> {
            Optional<DatabaseHealthCheck> lastSuccess = healthCheckRepo
                    .findTopByDatabaseIdAndSuccessTrueOrderByCheckedAtDesc(dbId);
            Optional<DatabaseHealthCheck> lastFailure = healthCheckRepo
                    .findTopByDatabaseIdAndSuccessFalseOrderByCheckedAtDesc(dbId);
            Optional<DatabaseHealthCheck> latest = healthCheckRepo
                    .findTopByDatabaseIdOrderByCheckedAtDesc(dbId);

            String orgName = latest.map(DatabaseHealthCheck::getOrgName).orElse(dbId);
            Integer lastResponseTime = latest.map(DatabaseHealthCheck::getResponseTimeMs).orElse(null);
            boolean healthy = latest.map(DatabaseHealthCheck::isSuccess).orElse(false);

            return new DatabaseStatus(
                    dbId,
                    orgName,
                    lastSuccess.map(DatabaseHealthCheck::getCheckedAt).orElse(null),
                    lastFailure.map(DatabaseHealthCheck::getCheckedAt).orElse(null),
                    lastResponseTime,
                    healthy
            );
        }).collect(Collectors.toList());

        return new KeepAliveStatusResponse(
                lastExecutionTime, lastExecutionId,
                latestRun.size(), successes, failures, avgTime,
                dbStatuses
        );
    }

    private void logPingResult(String label, DatabasePingResult result) {
        if (result.success()) {
            log.info("[KEEPALIVE] ✓ {} — {}ms", label, result.responseTimeMs());
        } else {
            log.warn("[KEEPALIVE] ✗ {} — {}", label, result.errorMessage());
        }
    }

    private String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return (msg == null || msg.isBlank()) ? root.getClass().getSimpleName() : msg;
    }
}
