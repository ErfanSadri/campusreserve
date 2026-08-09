package com.erfansadri.campusreserve.reservation;

public class ReservationUnavailableException extends RuntimeException {

    public ReservationUnavailableException(String message) {
        super(message);
    }
}