CREATE TABLE waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    attendee_name VARCHAR(150) NOT NULL,
    attendee_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    promoted_at TIMESTAMPTZ,
    promoted_reservation_id BIGINT REFERENCES reservations(id),
    CONSTRAINT waitlist_entries_status_check
        CHECK (status IN ('WAITING', 'PROMOTED')),
    CONSTRAINT waitlist_entries_promotion_check
        CHECK (
            (status = 'WAITING' AND promoted_at IS NULL AND promoted_reservation_id IS NULL)
            OR
            (status = 'PROMOTED' AND promoted_at IS NOT NULL AND promoted_reservation_id IS NOT NULL)
        )
);

CREATE INDEX waitlist_entries_waiting_queue_idx
    ON waitlist_entries (event_id, created_at, id)
    WHERE status = 'WAITING';

CREATE UNIQUE INDEX waitlist_entries_one_active_waiter_idx
    ON waitlist_entries (event_id, LOWER(attendee_email))
    WHERE status = 'WAITING';
