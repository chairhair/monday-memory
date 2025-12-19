-- One row per Stripe event (idempotency guard)
CREATE INDEX ix_payment_event_user_id ON payment_event(user_id);
CREATE INDEX ix_payment_event_user_received_at ON payment_event(user_id, received_at);