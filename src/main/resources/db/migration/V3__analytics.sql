CREATE TABLE analytics_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  event_name TEXT NOT NULL,          -- e.g. 'session_start'
  result TEXT NOT NULL DEFAULT 'OK', -- 'OK' | 'DENIED' | 'ERROR'

  user_id TEXT NULL,                -- MM user id (nullable for guests)
  principal_type TEXT NOT NULL,      -- 'USER' | 'GUEST'
  principal_key TEXT NOT NULL,        -- stable id for either

  session_id TEXT NULL,

  http_result INTEGER NULL,
  error_code TEXT NULL,

  latency_ms INTEGER NULL,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ae_occurred_at ON analytics_event (occurred_at);
CREATE INDEX idx_ae_event_time ON analytics_event (event_name, occurred_at);
CREATE INDEX idx_ae_principal_time ON analytics_event (principal_id, occurred_at);
CREATE INDEX idx_ae_user_time ON analytics_event (user_id, occurred_at);
