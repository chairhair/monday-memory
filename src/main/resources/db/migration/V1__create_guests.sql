-- =========================
-- guests
-- =========================
CREATE TABLE guests (
    guestId UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- from GuestEntity snippet
    user_id UUID NOT NULL,
    guest_key VARCHAR(255) NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT now()

    CONSTRAINT uk_guest_guest_key UNIQUE (guest_key)

    CONSTRAINT uk_guest_source UNIQUE (source)

    CONSTRAINT fk_guests_user
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_guests_guest_key ON guests(guest_key);
CREATE INDEX ix_guests_user_id ON guests(user_id);