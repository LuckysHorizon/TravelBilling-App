-- ═══════════════════════════════════════════════════════════════
-- V16: Database Health Check table for keep-alive monitoring
-- ═══════════════════════════════════════════════════════════════
-- Stores ping results for super-admin and all tenant databases.
-- Each keep-alive execution groups its pings under a single UUID.

CREATE TABLE database_health_checks (
    id                BIGSERIAL PRIMARY KEY,
    database_id       VARCHAR(100)  NOT NULL,
    org_id            BIGINT,
    org_name          VARCHAR(255),
    org_slug          VARCHAR(100),
    checked_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    success           BOOLEAN       NOT NULL,
    response_time_ms  INT,
    error_message     TEXT,
    execution_id      UUID          NOT NULL
);

CREATE INDEX idx_health_checks_db_id        ON database_health_checks(database_id);
CREATE INDEX idx_health_checks_checked_at   ON database_health_checks(checked_at);
CREATE INDEX idx_health_checks_execution_id ON database_health_checks(execution_id);
CREATE INDEX idx_health_checks_success      ON database_health_checks(success);
