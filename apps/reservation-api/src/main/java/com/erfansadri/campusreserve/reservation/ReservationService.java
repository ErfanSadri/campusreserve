package com.erfansadri.campusreserve.reservation;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventNotFoundException;
import com.erfansadri.campusreserve.event.EventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(
                    ReservationStatus.HELD,
                    ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ReservationResponse createHold(
            Long eventId,
            CreateReservationRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(event.getRegistrationOpensAt())) {
            throw new ReservationUnavailableException(
                    "Registration is not open yet.");
        }

        if (!now.isBefore(event.getStartTime())) {
            throw new ReservationUnavailableException(
                    "Registration is closed for this event.");
        }

        String normalizedEmail = request.attendeeEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        boolean alreadyReserved = reservationRepository
                .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                        eventId,
                        normalizedEmail,
                        ACTIVE_STATUSES)
                .isPresent();

        if (alreadyReserved) {
            throw new DuplicateReservationException();
        }

        if (!event.hasAvailableCapacity()) {
            throw new ReservationUnavailableException(
                    "Event has no remaining capacity.");
        }

        event.reserveSpot();

        Reservation reservation = new Reservation(
                event,
                request.attendeeName().trim(),
                normalizedEmail,
                now.plus(HOLD_DURATION));

        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(
                        () -> new ReservationNotFoundException(reservationId));

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(
                        () -> new ReservationNotFoundException(reservationId));

        if (reservation.getHeldUntil() != null
                && !OffsetDateTime.now().isBefore(reservation.getHeldUntil())) {
            throw new ReservationUnavailableException(
                    "Reservation hold has expired.");
        }

        reservation.confirm();

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(
                        () -> new ReservationNotFoundException(reservationId));

        reservation.cancel();
        reservation.getEvent().releaseSpot();

        return ReservationResponse.from(reservation);
    }
}