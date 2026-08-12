package com.erfansadri.campusreserve.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.erfansadri.campusreserve.reservation.Reservation;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleTopic;

import org.springframework.stereotype.Component;

@Component
public class JpaOutboxEventRecorder implements OutboxEventRecorder {

    private static final String RESERVATION_AGGREGATE_TYPE = "reservation";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventCodec outboxEventCodec;

    public JpaOutboxEventRecorder(
            OutboxEventRepository outboxEventRepository,
            OutboxEventCodec outboxEventCodec) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventCodec = outboxEventCodec;
    }

    @Override
    public void recordHoldCreated(
            Reservation reservation,
            OffsetDateTime occurredAt) {
        UUID outboxEventId = UUID.randomUUID();
        record(ReservationLifecycleEvent.holdCreated(
                outboxEventId,
                reservation,
                occurredAt));
    }

    @Override
    public void recordConfirmed(
            Reservation reservation,
            OffsetDateTime occurredAt) {
        UUID outboxEventId = UUID.randomUUID();
        record(ReservationLifecycleEvent.confirmed(
                outboxEventId,
                reservation,
                occurredAt));
    }

    @Override
    public void recordCancelled(
            Reservation reservation,
            OffsetDateTime occurredAt) {
        UUID outboxEventId = UUID.randomUUID();
        record(ReservationLifecycleEvent.cancelled(
                outboxEventId,
                reservation,
                occurredAt));
    }

    @Override
    public void recordExpired(
            Reservation reservation,
            OffsetDateTime occurredAt) {
        UUID outboxEventId = UUID.randomUUID();
        record(ReservationLifecycleEvent.expired(
                outboxEventId,
                reservation,
                occurredAt));
    }

    private void record(ReservationLifecycleEvent event) {
        outboxEventRepository.save(new OutboxEvent(
                event.outboxEventId(),
                RESERVATION_AGGREGATE_TYPE,
                event.reservationId(),
                event.eventType(),
                event.eventVersion(),
                ReservationLifecycleTopic.NAME,
                outboxEventCodec.serialize(event),
                event.occurredAt()));
    }
}
