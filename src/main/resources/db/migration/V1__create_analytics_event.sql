-- -------------------------
-- analytics_event
-- -------------------------

CREATE TABLE analytics_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    event_name VARCHAR(255) NOT NULL,

    user_id UUID NOT NULL,

    principal_key TEXT,
    principal_type VARCHAR(255),

    session_id TEXT,

    http_result INTEGER,
    error_code TEXT,
    latency_ms BIGINT,

    created_at  TIMESTAMP NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);

ALTER TABLE analytics_event
ADD CONSTRAINT fk_analytics_event_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

CREATE INDEX idx_analytics_event_user_id     ON analytics_event(user_id);
CREATE INDEX idx_analytics_event_occurred_at ON analytics_event(occurred_at);
CREATE INDEX idx_analytics_event_event_name  ON analytics_event(event_name);
CREATE INDEX idx_analytics_event_session_id  ON analytics_event(session_id);
