CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    attendee_name VARCHAR(150) NOT NULL,
    attendee_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    held_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT reservations_event_fk
        FOREIGN KEY (event_id)
        REFERENCES events (id),

    CONSTRAINT reservations_status_check
        CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),

    CONSTRAINT held_reservation_requires_expiration
        CHECK (
            status <> 'HELD'
            OR held_until IS NOT NULL
        )
);

CREATE INDEX idx_reservations_event_id
    ON reservations (event_id);

CREATE INDEX idx_reservations_status
    ON reservations (status);

CREATE UNIQUE INDEX idx_reservations_active_event_email
    ON reservations (event_id, LOWER(attendee_email))
    WHERE status IN ('HELD', 'CONFIRMED');