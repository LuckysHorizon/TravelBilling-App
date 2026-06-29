package com.travelbillpro.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the Keep-Alive subsystem.
 */
public final class KeepAliveDto {

    private KeepAliveDto() {} // utility class

    // ── Request / trigger response ──────────────────────────────

    /**
     * Result of a single keep-alive execution run.
     */
    public record KeepAliveResult(
            UUID executionId,
            int totalDatabases,
            int successes,
            int failures,
            long averageResponseTimeMs,
            LocalDateTime executedAt,
            List<DatabasePingResult> details
    ) {}

    /**
     * Ping result for a single database.
     */
    public record DatabasePingResult(
            String databaseId,
            String orgName,
            boolean success,
            Integer responseTimeMs,
            String errorMessage
    ) {}

    // ── Status endpoint response ────────────────────────────────

    /**
     * Overall keep-alive status summary.
     */
    public record KeepAliveStatusResponse(
            LocalDateTime lastExecutionTime,
            UUID lastExecutionId,
            int totalDatabasesChecked,
            int successes,
            int failures,
            long averageResponseTimeMs,
            List<DatabaseStatus> databases
    ) {}

    /**
     * Per-database health status.
     */
    public record DatabaseStatus(
            String databaseId,
            String orgName,
            LocalDateTime lastSuccess,
            LocalDateTime lastFailure,
            Integer lastResponseTimeMs,
            boolean healthy
    ) {}
}
