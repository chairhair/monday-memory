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
