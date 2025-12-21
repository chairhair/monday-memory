-- -------------------------
-- session_options
-- -------------------------
CREATE TABLE session_options (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_scope       VARCHAR(255) NOT NULL,
  max_chunks_per_session INTEGER
);
