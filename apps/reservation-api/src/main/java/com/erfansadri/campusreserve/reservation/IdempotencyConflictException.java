package com.erfansadri.campusreserve.reservation;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("Idempotency key was already used with a different request.");
    }
}