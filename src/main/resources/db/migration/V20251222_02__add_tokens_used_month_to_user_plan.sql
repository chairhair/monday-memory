ALTER TABLE user_plan
ADD COLUMN IF NOT EXISTS tokens_used_month BIGINT DEFAULT 0;

-- Initialize existing rows to "current month" so nothing breaks immediately.
UPDATE user_plan
SET tokens_used_month = (EXTRACT(YEAR FROM NOW())::int * 100 + EXTRACT(MONTH FROM NOW())::int)
WHERE tokens_used_month IS NULL;

CREATE INDEX IF NOT EXISTS ix_user_plan_tokens_used_month
    ON user_plan (tokens_used_month);
