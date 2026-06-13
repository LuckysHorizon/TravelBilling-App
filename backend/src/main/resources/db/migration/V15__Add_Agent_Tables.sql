-- =========================================================
-- V15: Agent AI Tables
-- Agent sessions and messages for the AI assistant feature.
-- These tables are tenant-aware (exist in each tenant DB).
-- =========================================================

CREATE TABLE IF NOT EXISTS agent_sessions (
    id              BIGSERIAL PRIMARY KEY,
    session_id      UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255),
    metadata        JSONB DEFAULT '{}',
    provider_used   VARCHAR(50),
    total_tokens    INTEGER DEFAULT 0,
    message_count   INTEGER DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS agent_messages (
    id                BIGSERIAL PRIMARY KEY,
    message_id        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    session_id        UUID NOT NULL REFERENCES agent_sessions(session_id) ON DELETE CASCADE,
    role              VARCHAR(20) NOT NULL,
    content           TEXT,
    tool_calls        JSONB,
    tool_results      JSONB,
    token_count       INTEGER DEFAULT 0,
    provider          VARCHAR(50),
    model             VARCHAR(100),
    latency_ms        INTEGER,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_sessions_user ON agent_sessions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_messages_session ON agent_messages(session_id, created_at);
