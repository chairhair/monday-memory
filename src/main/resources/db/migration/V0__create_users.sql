CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- users
-- =========================
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
-- users external account
-- =========================
CREATE TABLE user_external_account (
    id           BIGSERIAL PRIMARY KEY,

    user_id      UUID NOT NULL,

    provider     VARCHAR(64) NOT NULL,
    external_id  VARCHAR(255) NOT NULL,

    created_at   TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_external_account_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,

    -- Prevent duplicates per user/provider (e.g., one DISCORD account per user)
    CONSTRAINT uk_user_external_account_user_provider
        UNIQUE (user_id, provider),

    -- Prevent the same external identity from being linked twice for the same provider
    CONSTRAINT uk_user_external_account_provider_external_id
        UNIQUE (provider, external_id)
);

CREATE INDEX ix_user_external_account_user_id
    ON user_external_account(user_id);

CREATE INDEX ix_user_external_account_provider_external_id
    ON user_external_account(provider, external_id);

-- =========================
-- user_credentials
-- =========================
CREATE TABLE user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    password TEXT,

    user_id UUID NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_credentials_user
            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
