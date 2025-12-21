-- -------------------------
-- session_memory
-- -------------------------

CREATE TABLE session_memory (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    source VARCHAR(64) NOT NULL,
    source_conversation VARCHAR(128) NOT NULL,

    principal_type VARCHAR(16) NOT NULL,
    principal_id   VARCHAR(64) NOT NULL,

    session_state VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ended_at   TIMESTAMP,

    chunk_count INTEGER NOT NULL,

    idempotency_key TEXT,

    session_options_id UUID,

    scope VARCHAR(20) NOT NULL,

    last_occurred_at TIMESTAMP,

    topic_id BIGINT,

    version BIGINT NOT NULL DEFAULT 0
);

-- Foreign keys
-- Change "users" to your actual UserEntity table name.
ALTER TABLE session_memory
ADD CONSTRAINT fk_session_memory_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

-- SessionOptionsEntity is @OneToOne(cascade=ALL, orphanRemoval=true)
-- DB-level ON DELETE CASCADE matches that intent.
ALTER TABLE session_memory
ADD CONSTRAINT fk_session_memory_session_options
FOREIGN KEY (session_options_id)
REFERENCES session_options(id)
ON DELETE CASCADE;

-- Optional: enforce one-to-one uniqueness (recommended)
-- Ensures no two sessions point at the same options row.
CREATE UNIQUE INDEX ux_session_memory_session_options_id
ON session_memory(session_options_id)
WHERE session_options_id IS NOT NULL;

-- Indexes (match @Table(indexes=...))
-- NOTE: In your annotation you used "principalId" etc, but the actual column names are
-- principal_id, session_id, source, source_conversation.
CREATE INDEX idx_session_identity
ON session_memory(principal_id, session_id, source, source_conversation);

CREATE INDEX idx_session_last_occurred
ON session_memory(last_occurred_at);

CREATE INDEX idx_session_idempotency
ON session_memory(principal_id, source, source_conversation, idempotency_key, session_state);
