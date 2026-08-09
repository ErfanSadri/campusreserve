package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        OffsetDateTime startTime,
        OffsetDateTime registrationOpensAt,
        int capacity,
        int remainingCapacity,
        OffsetDateTime createdAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getRegistrationOpensAt(),
                event.getCapacity(),
                event.getRemainingCapacity(),
                event.getCreatedAt());
    }
}