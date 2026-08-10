package com.erfansadri.campusreserve.reservation.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.erfansadri.campusreserve.reservation.Reservation;
import com.erfansadri.campusreserve.reservation.ReservationStatus;

public record ReservationLifecycleEvent(
        UUID outboxEventId,
        String eventType,
        String eventVersion,
        Long eventId,
        Long reservationId,
        ReservationStatus reservationStatus,
        String attendeeEmail,
        OffsetDateTime occurredAt) {

    public static final String VERSION = "v1";

    public static ReservationLifecycleEvent holdCreated(
            UUID outboxEventId,
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from(
                outboxEventId,
                "reservation.hold.created",
                reservation,
                occurredAt);
    }

    public static ReservationLifecycleEvent confirmed(
            UUID outboxEventId,
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from(
                outboxEventId,
                "reservation.confirmed",
                reservation,
                occurredAt);
    }

    public static ReservationLifecycleEvent cancelled(
            UUID outboxEventId,
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from(
                outboxEventId,
                "reservation.cancelled",
                reservation,
                occurredAt);
    }

    private static ReservationLifecycleEvent from(
            UUID outboxEventId,
            String eventType,
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return new ReservationLifecycleEvent(
                outboxEventId,
                eventType,
                VERSION,
                reservation.getEvent().getId(),
                reservation.getId(),
                reservation.getStatus(),
                reservation.getAttendeeEmail(),
                occurredAt);
    }
}
