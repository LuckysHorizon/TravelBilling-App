package com.travelbillpro.service;

import com.travelbillpro.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for database keep-alive pings.
 * Runs every 6 hours by default (configurable via app.keepalive.cron).
 * Keeps scheduling concerns separate from business logic in KeepAliveService.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KeepAliveScheduler {

    private final KeepAliveService keepAliveService;

    @Value("${app.keepalive.enabled:true}")
    private boolean enabled;

    /**
     * Scheduled keep-alive execution.
     * Default: every 6 hours (0 0 * /6 * * *).
     * Runs outside any HTTP request, so TenantContext is explicitly cleared
     * to ensure master DB is used for organization queries.
     */
    @Scheduled(cron = "${app.keepalive.cron:0 0 */6 * * *}")
    public void run() {
        if (!enabled) {
            log.debug("[KEEPALIVE] Scheduler is disabled, skipping execution");
            return;
        }

        log.info("[KEEPALIVE] Scheduled execution starting...");

        // Ensure clean tenant context — this runs outside HTTP requests
        TenantContext.clear();

        try {
            keepAliveService.executeKeepAlive();
            keepAliveService.cleanupOldRecords();
        } catch (Exception e) {
            log.error("[KEEPALIVE] Scheduled execution failed", e);
        } finally {
            TenantContext.clear();
        }
    }
}
