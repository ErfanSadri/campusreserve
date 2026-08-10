CREATE TABLE processed_consumer_events (
    id BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    outbox_event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT processed_consumer_events_consumer_event_unique
        UNIQUE (consumer_name, outbox_event_id)
);
