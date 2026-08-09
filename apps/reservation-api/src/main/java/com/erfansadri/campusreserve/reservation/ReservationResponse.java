package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;

public record ReservationResponse(
        Long id,
        Long eventId,
        String attendeeName,
        String attendeeEmail,
        ReservationStatus status,
        OffsetDateTime heldUntil,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getAttendeeName(),
                reservation.getAttendeeEmail(),
                reservation.getStatus(),
                reservation.getHeldUntil(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}