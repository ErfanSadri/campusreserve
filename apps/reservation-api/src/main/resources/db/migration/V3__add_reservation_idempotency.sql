ALTER TABLE reservations
    ADD COLUMN idempotency_key VARCHAR(128),
    ADD COLUMN request_fingerprint CHAR(64);

ALTER TABLE reservations
    ADD CONSTRAINT reservation_idempotency_pair_check
    CHECK (
        (idempotency_key IS NULL AND request_fingerprint IS NULL)
        OR
        (idempotency_key IS NOT NULL AND request_fingerprint IS NOT NULL)
    );

CREATE UNIQUE INDEX idx_reservations_event_idempotency_key
    ON reservations (event_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;