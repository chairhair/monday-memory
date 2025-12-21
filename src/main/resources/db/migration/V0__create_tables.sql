CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Users
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320),
    display_name VARCHAR(255),
    linked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- =========================
-- user_credentials
-- =========================
CREATE TABLE IF NOT EXISTS user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    password TEXT,

    user_id UUID NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()

    CONSTRAINT fk_user_credentials_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- guests
-- =========================
CREATE TABLE IF NOT EXISTS guests (
    guestId UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- from GuestEntity snippet
    user_id UUID NOT NULL,
    guest_key VARCHAR(255) NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT now()

    CONSTRAINT uk_guest_guest_key UNIQUE (guest_key)

    CONSTRAINT uk_guest_source UNIQUE (source)

    CONSTRAINT fk_guests_user
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_guests_guest_key ON guests(guest_key);
CREATE INDEX IF NOT EXISTS ix_guests_user_id ON guests(user_id);


-- =========================
-- tokens
-- =========================

CREATE TABLE IF NOT EXISTS tokens_entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_credentials_id UUID NULL,

    token VARCHAR(255) NOT NULL UNIQUE,

    access_level VARCHAR(64) NULL,
    time_created TIMESTAMP NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT false,
    revoked BOOLEAN NOT NULL DEFAULT false

    CONSTRAINT fk_tokens_user_credentials FOREIGN KEY (user_credentials_id) REFERENCES user_credentials(id)
);

ALTER TABLE tokens_entity
    ADD CONSTRAINT fk_tokens_user_credentials
    FOREIGN KEY (user_credentials_id)
    REFERENCES user_credentials(id)
    ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS ix_tokens_user_id ON tokens_entity(user_id);
CREATE INDEX IF NOT EXISTS ix_tokens_user_credentials_id ON tokens_entity(user_credentials_id);

-- =========================
-- options
-- =========================
-- Vx__create_options_tables.sql
-- Postgres migration for OptionsEntity + its ElementCollections.

-- If you use UUID generation defaults in SQL:
CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
REFERENCES users(id)
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_options_user_id ON options(user_id);

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

-- -------------------------
-- session_options
-- -------------------------
CREATE TABLE session_options (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_scope       VARCHAR(255) NOT NULL,
  max_chunks_per_session INTEGER
);

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

-- -------------------------
-- role
-- -------------------------
CREATE TABLE role_entity (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    access_level VARCHAR(255) NOT NULL,
    CONSTRAINT uk_role_entity_access_level UNIQUE (access_level)
);

-- User ↔ Roles join table
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES role_entity(id) ON DELETE CASCADE
);

-- Helpful indexes (optional but smart)
CREATE INDEX ix_users_email ON users(email);
CREATE INDEX ix_user_roles_role_id ON user_roles(role_id);

-- -------------------------
-- payment_events
-- -------------------------
CREATE TABLE payment_event (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    stripe_event_id VARCHAR(255) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json TEXT,
    CONSTRAINT uk_payment_event_stripe_event_id UNIQUE (stripe_event_id),
    CONSTRAINT fk_payment_event_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

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

-- -------------------------
-- price_plan
-- -------------------------
CREATE TABLE price_plan (
    id UUID PRIMARY KEY,

    code VARCHAR(255) NOT NULL,
    stripe_price_id VARCHAR(255) NOT NULL,

    display_name VARCHAR(255),
    monthly_amount INTEGER,
    annual_amount INTEGER,

    max_topics_per_period INTEGER,
    max_tokens_per_period BIGINT,

    warning_threshold_ratio DOUBLE PRECISION,

    CONSTRAINT uk_price_plan_code UNIQUE (code),
    CONSTRAINT uk_price_plan_stripe_price_id UNIQUE (stripe_price_id)
);

-- Optional but useful (lookup by code is common)
CREATE INDEX ix_price_plan_code ON price_plan(code);

-- -------------------------
-- user_plan
-- -------------------------
CREATE TABLE user_plan (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    price_plan_id UUID,

    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),

    current_period_end TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    period_start TIMESTAMP WITH TIME ZONE,
    period_end TIMESTAMP WITH TIME ZONE,

    topics_used INTEGER,
    tokens_used BIGINT,

    version BIGINT NOT NULL,

    CONSTRAINT fk_user_plan_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,

    CONSTRAINT fk_user_plan_price_plan
        FOREIGN KEY (price_plan_id) REFERENCES price_plan(id),

    CONSTRAINT uk_user_plan_user_id
        UNIQUE (user_id),

    CONSTRAINT ix_user_plan_customer_id
        UNIQUE (stripe_customer_id)
);

-- Optional but useful for subscription lookups (Stripe subscription id is frequently queried)
CREATE INDEX ix_user_plan_subscription_id
    ON user_plan(stripe_subscription_id);


ALTER TABLE user_plan
    ADD CONSTRAINT uk_user_plan_stripe_subscription_id UNIQUE (stripe_subscription_id);


