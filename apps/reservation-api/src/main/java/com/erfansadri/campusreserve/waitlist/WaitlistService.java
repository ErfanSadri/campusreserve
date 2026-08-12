package com.erfansadri.campusreserve.waitlist;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventNotFoundException;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.reservation.ReservationRepository;
import com.erfansadri.campusreserve.reservation.ReservationStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitlistService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.HELD, ReservationStatus.CONFIRMED);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final CampusReserveMetrics metrics;

    public WaitlistService(
            WaitlistEntryRepository waitlistEntryRepository,
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            CampusReserveMetrics metrics) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.metrics = metrics;
    }

    @Transactional
    public WaitlistEntryResponse join(Long eventId, JoinWaitlistRequest request) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(event.getRegistrationOpensAt())) {
            throw new WaitlistUnavailableException("Registration is not open yet.");
        }
        if (!now.isBefore(event.getStartTime())) {
            throw new WaitlistUnavailableException("Registration is closed for this event.");
        }
        if (event.hasAvailableCapacity()) {
            throw new WaitlistUnavailableException(
                    "Event has available capacity; create a reservation instead.");
        }

        String normalizedName = request.attendeeName().trim();
        String normalizedEmail = request.attendeeEmail().trim().toLowerCase(Locale.ROOT);
        if (reservationRepository
                .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                        eventId, normalizedEmail, ACTIVE_STATUSES)
                .isPresent()) {
            throw new WaitlistUnavailableException(
                    "Attendee already has an active reservation for this event.");
        }
        if (waitlistEntryRepository
                .existsByEvent_IdAndAttendeeEmailIgnoreCaseAndStatus(
                        eventId, normalizedEmail, WaitlistStatus.WAITING)) {
            throw new WaitlistUnavailableException(
                    "Attendee is already waiting for this event.");
        }

        WaitlistEntry saved = waitlistEntryRepository.save(
                new WaitlistEntry(event, normalizedName, normalizedEmail));
        metrics.waitlistEntryCreated();
        return WaitlistEntryResponse.from(saved);
    }
}
