CREATE TABLE topic (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    summary TEXT,

    status VARCHAR(16) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,

    first_seen_at TIMESTAMP NOT NULL,
    last_used_at  TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_topic_user_name UNIQUE (user_id, name)
);

-- Change "users" to your actual UserEntity table name.
ALTER TABLE topic
ADD CONSTRAINT fk_topic_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

-- Indexes per @Table(indexes=...)
CREATE INDEX idx_topic_user ON topic(user_id);
CREATE INDEX idx_topic_status ON topic(status);
CREATE INDEX idx_topic_last_used_at ON topic(last_used_at);