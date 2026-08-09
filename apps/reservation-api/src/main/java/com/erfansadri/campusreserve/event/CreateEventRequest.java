package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(

        @NotBlank
        @Size(max = 200)
        String title,

        @Size(max = 5000)
        String description,

        @NotBlank
        @Size(max = 255)
        String location,

        @NotNull
        OffsetDateTime startTime,

        @NotNull
        OffsetDateTime registrationOpensAt,

        @Positive
        int capacity) {
}