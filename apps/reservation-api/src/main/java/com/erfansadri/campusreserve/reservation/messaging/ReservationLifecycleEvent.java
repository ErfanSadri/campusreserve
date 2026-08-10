package com.erfansadri.campusreserve.reservation.messaging;

import java.time.OffsetDateTime;

import com.erfansadri.campusreserve.reservation.Reservation;
import com.erfansadri.campusreserve.reservation.ReservationStatus;

public record ReservationLifecycleEvent(
        String eventType,
        String eventVersion,
        Long eventId,
        Long reservationId,
        ReservationStatus reservationStatus,
        String attendeeEmail,
        OffsetDateTime occurredAt) {

    public static final String VERSION = "v1";

    public static ReservationLifecycleEvent holdCreated(
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from("reservation.hold.created", reservation, occurredAt);
    }

    public static ReservationLifecycleEvent confirmed(
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from("reservation.confirmed", reservation, occurredAt);
    }

    public static ReservationLifecycleEvent cancelled(
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return from("reservation.cancelled", reservation, occurredAt);
    }

    private static ReservationLifecycleEvent from(
            String eventType,
            Reservation reservation,
            OffsetDateTime occurredAt) {

        return new ReservationLifecycleEvent(
                eventType,
                VERSION,
                reservation.getEvent().getId(),
                reservation.getId(),
                reservation.getStatus(),
                reservation.getAttendeeEmail(),
                occurredAt);
    }
}
