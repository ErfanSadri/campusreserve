package com.erfansadri.campusreserve.waitlist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByEvent_IdAndAttendeeEmailIgnoreCaseAndStatus(
            Long eventId, String attendeeEmail, WaitlistStatus status);

    List<WaitlistEntry> findByEvent_IdAndStatusOrderByCreatedAtAscIdAsc(
            Long eventId, WaitlistStatus status);
}
