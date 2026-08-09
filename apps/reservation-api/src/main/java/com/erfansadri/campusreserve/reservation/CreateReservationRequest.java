package com.erfansadri.campusreserve.reservation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(

        @NotBlank
        @Size(max = 150)
        String attendeeName,

        @NotBlank
        @Email
        @Pattern(
                regexp = "^[^\\s@<>\\[\\]()]+@[^\\s@<>\\[\\]()]+\\.[^\\s@<>\\[\\]()]+$",
                message = "must be a valid email address")
        @Size(max = 320)
        String attendeeEmail) {
}