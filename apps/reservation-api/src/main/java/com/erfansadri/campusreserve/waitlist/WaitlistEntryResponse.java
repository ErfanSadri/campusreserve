package com.erfansadri.campusreserve.waitlist;

import java.time.OffsetDateTime;

public record WaitlistEntryResponse(
        Long id,
        Long eventId,
        String attendeeName,
        String attendeeEmail,
        WaitlistStatus status,
        OffsetDateTime createdAt) {

    public static WaitlistEntryResponse from(WaitlistEntry entry) {
        return new WaitlistEntryResponse(
                entry.getId(), entry.getEvent().getId(), entry.getAttendeeName(),
                entry.getAttendeeEmail(), entry.getStatus(), entry.getCreatedAt());
    }
}
