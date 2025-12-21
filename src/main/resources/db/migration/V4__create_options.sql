-- =========================
-- options
-- =========================
CREATE TABLE options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- LLM config
    model_name   TEXT,
    temperature  DOUBLE PRECISION,
    top_p        DOUBLE PRECISION,
    max_tokens   INTEGER,

    -- memory config
    enable_memory    BOOLEAN NOT NULL DEFAULT FALSE,
    include_topics   BOOLEAN NOT NULL DEFAULT FALSE,
    include_sessions BOOLEAN NOT NULL DEFAULT FALSE,
    max_memory_chunks INTEGER,

    -- pre-inject payload
    pre_inject TEXT,

    -- conversation continuity
    continue_conversation BOOLEAN NOT NULL DEFAULT FALSE,
    conversation_id       TEXT,

    -- ownership
    user_id UUID NULL
);

-- If options truly "matches with user 1:1", make this UNIQUE.
-- (Your entity currently uses @ManyToOne, not @OneToOne, but your comment says 1:1.)
CREATE UNIQUE INDEX ux_options_user_id ON options(user_id);

-- Add FK once your users table name is confirmed.
-- CHANGE "users" to your actual UserEntity table.
ALTER TABLE options
ADD CONSTRAINT fk_options_user
FOREIGN KEY (user_id)
REFERENCES users(user_id)
ON DELETE CASCADE;

-- =========================
-- options_topic_ids
-- =========================
CREATE TABLE options_topic_ids (
    option_id UUID NOT NULL,
    topic_id  UUID NOT NULL,

    CONSTRAINT fk_options_topic_ids_option
      FOREIGN KEY (option_id) REFERENCES options(id)
      ON DELETE CASCADE
);

-- Prevent duplicates in the list
ALTER TABLE options_topic_ids
ADD CONSTRAINT pk_options_topic_ids PRIMARY KEY (option_id, topic_id);

CREATE INDEX ix_options_topic_ids_topic_id ON options_topic_ids(topic_id);

-- =========================
-- options_session_ids
-- =========================
CREATE TABLE options_session_ids (
    option_id  UUID NOT NULL,
    session_id UUID NOT NULL,

    CONSTRAINT fk_options_session_ids_option
      FOREIGN KEY (option_id) REFERENCES options(id)
      ON DELETE CASCADE
);

-- Prevent duplicates in the list
ALTER TABLE options_session_ids
ADD CONSTRAINT pk_options_session_ids PRIMARY KEY (option_id, session_id);

CREATE INDEX ix_options_session_ids_session_id ON options_session_ids(session_id);
