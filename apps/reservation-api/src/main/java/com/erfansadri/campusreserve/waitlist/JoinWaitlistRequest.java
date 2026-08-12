package com.erfansadri.campusreserve.waitlist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinWaitlistRequest(
        @NotBlank @Size(max = 150) String attendeeName,
        @NotBlank @Email @Size(max = 320) String attendeeEmail) {
}
