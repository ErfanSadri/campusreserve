package com.erfansadri.campusreserve.event;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long id) {
        super("Event " + id + " was not found.");
    }
}