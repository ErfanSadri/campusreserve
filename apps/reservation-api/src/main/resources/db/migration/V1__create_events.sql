CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    registration_opens_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    remaining_capacity INTEGER NOT NULL CHECK (remaining_capacity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT remaining_capacity_within_capacity
        CHECK (remaining_capacity <= capacity)
);

CREATE INDEX idx_events_start_time
    ON events (start_time);