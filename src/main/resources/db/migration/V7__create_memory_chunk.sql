-- -------------------------
-- memory_chunk
-- -------------------------
CREATE TABLE memory_chunk (
  memory_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  session_id    UUID NOT NULL,
  occurred_at   TIMESTAMP NOT NULL,
  ingested_at   TIMESTAMP NOT NULL,

  tags          TEXT,    -- JsonStringListConverter writes text
  content       TEXT,    -- JsonMapConverter writes text (upgrade to JSONB later)

  hash_sha256   VARCHAR(64) NOT NULL,

  CONSTRAINT fk_chunk_session
    FOREIGN KEY (session_id) REFERENCES session_memory(session_id)
);