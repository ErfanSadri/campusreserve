package com.erfansadri.campusreserve.reservation;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long id) {
        super("Reservation " + id + " was not found.");
    }
}