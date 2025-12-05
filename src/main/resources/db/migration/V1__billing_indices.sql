-- One row per Stripe event (idempotency guard)
CREATE INDEX ix_payment_event_user_id ON payment_event(user_id);

-- User plan lookups: by user id and by Stripe customer id
CREATE UNIQUE INDEX uk_user_plan_user_id on user_plan(user_id);
CREATE INDEX ix_user_plan_customer_id ON user_plan(stripe_customer_id);