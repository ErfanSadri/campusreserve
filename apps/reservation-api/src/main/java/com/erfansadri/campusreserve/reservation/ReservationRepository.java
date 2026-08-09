package com.erfansadri.campusreserve.reservation;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
            Long eventId,
            String attendeeEmail,
            Collection<ReservationStatus> statuses);
}