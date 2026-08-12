package com.erfansadri.campusreserve.outbox;

import java.time.OffsetDateTime;

import com.erfansadri.campusreserve.reservation.Reservation;

public interface OutboxEventRecorder {

    void recordHoldCreated(Reservation reservation, OffsetDateTime occurredAt);

    void recordConfirmed(Reservation reservation, OffsetDateTime occurredAt);

    void recordCancelled(Reservation reservation, OffsetDateTime occurredAt);

    void recordExpired(Reservation reservation, OffsetDateTime occurredAt);
}
