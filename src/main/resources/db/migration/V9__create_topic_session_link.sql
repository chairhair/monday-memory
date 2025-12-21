-- TODO: Future development work will be done on this later to establish a repo
CREATE TABLE topic_session_link (
    id BIGSERIAL PRIMARY KEY,

    user_id UUID NOT NULL,

    topic_id UUID NOT NULL,
    session_id UUID NOT NULL,

    link_type VARCHAR(16) NOT NULL,
    similarity_score DOUBLE PRECISION,
    is_outlier BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_topic_session_link_topic_session UNIQUE (topic_id, session_id),

    CONSTRAINT fk_topic_session_link_topic
      FOREIGN KEY (topic_id) REFERENCES topic(id)
      ON DELETE CASCADE,

    CONSTRAINT fk_topic_session_link_session
      FOREIGN KEY (session_id) REFERENCES session_memory(session_id)
      ON DELETE CASCADE
);

-- Change "users" to your actual UserEntity table name.
ALTER TABLE topic_session_link
ADD CONSTRAINT fk_topic_session_link_user
FOREIGN KEY (user_id)
REFERENCES users(user_id)
ON DELETE CASCADE;

-- Useful indexes
CREATE INDEX idx_topic_session_link_user ON topic_session_link(user_id);
CREATE INDEX idx_topic_session_link_topic ON topic_session_link(topic_id);
CREATE INDEX idx_topic_session_link_session ON topic_session_link(session_id);
CREATE INDEX idx_topic_session_link_link_type ON topic_session_link(link_type);
