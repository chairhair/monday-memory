-- =========================
-- tokens
-- =========================
CREATE TABLE tokens_entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_credentials_id UUID NULL,

    token VARCHAR(255) NOT NULL UNIQUE,

    access_level VARCHAR(64) NULL,
    time_created TIMESTAMP NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT false,
    revoked BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT fk_tokens_user_credentials FOREIGN KEY (user_credentials_id) REFERENCES user_credentials(id)
);

ALTER TABLE tokens_entity
    ADD CONSTRAINT fk_tokens_user_credentials
    FOREIGN KEY (user_credentials_id)
    REFERENCES user_credentials(id)
    ON DELETE CASCADE;

CREATE INDEX ix_tokens_user_id ON tokens_entity(user_id);
CREATE INDEX ix_tokens_user_credentials_id ON tokens_entity(user_credentials_id);
