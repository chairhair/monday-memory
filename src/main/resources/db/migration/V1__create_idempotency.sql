-- -------------------------
-- idempotency
-- -------------------------
CREATE TABLE idempotency (
  idempotency_key VARCHAR(150) PRIMARY KEY,
  request_hash    VARCHAR(64)  NOT NULL,
  status_code     INTEGER      NOT NULL,
  response_json   TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT now(),
  expires_at      TIMESTAMP
);

CREATE INDEX idx_idempotency_expires_at ON idempotency(expires_at);
