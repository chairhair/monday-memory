CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL UNIQUE,

    -- enums stored as strings
    session_scope VARCHAR(255) NOT NULL,
    comm_scope    VARCHAR(20)  NOT NULL,

    max_chunks_per_session INTEGER,
    max_tokens_per_session INTEGER
);

-- FK name matches your @ForeignKey(name="fk_user_preferences")
-- Change "users" to your actual UserEntity table name.
ALTER TABLE user_preferences
ADD CONSTRAINT fk_user_preferences
FOREIGN KEY (user_id)
REFERENCES users(user_id)
ON DELETE CASCADE;

-- Helpful index (unique already creates an index, but this is redundant-safe if you ever drop unique)
-- CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);