package com.erfansadri.campusreserve.reservation;

public class DuplicateReservationException extends RuntimeException {

    public DuplicateReservationException() {
        super("Attendee already has an active reservation for this event.");
    }
}