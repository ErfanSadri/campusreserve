package com.erfansadri.campusreserve.reservation;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventCache;
import com.erfansadri.campusreserve.event.EventNotFoundException;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    public static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(
                    ReservationStatus.HELD,
                    ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final EventCache eventCache;
    private final OutboxEventRecorder outboxEventRecorder;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            EventCache eventCache,
            OutboxEventRecorder outboxEventRecorder) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
        this.outboxEventRecorder = outboxEventRecorder;
    }

    @Transactional
    public ReservationResponse createHold(
            Long eventId,
            String idempotencyKey,
            CreateReservationRequest request) {

        if (idempotencyKey == null
                || idempotencyKey.trim().isEmpty()
                || idempotencyKey.trim().length() > 128) {
            throw new InvalidIdempotencyKeyException();
        }

        String normalizedKey = idempotencyKey.trim();

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        String normalizedName = request.attendeeName().trim();

        String normalizedEmail = request.attendeeEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String requestFingerprint =
                ReservationRequestFingerprint.create(
                        eventId,
                        normalizedName,
                        normalizedEmail);

        var existingReservation = reservationRepository
                .findByEvent_IdAndIdempotencyKey(
                        eventId,
                        normalizedKey);

        if (existingReservation.isPresent()) {
            Reservation existing = existingReservation.orElseThrow();

            if (!existing.getRequestFingerprint()
                    .equals(requestFingerprint)) {
                throw new IdempotencyConflictException();
            }

            return ReservationResponse.from(existing);
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(event.getRegistrationOpensAt())) {
            throw new ReservationUnavailableException(
                    "Registration is not open yet.");
        }

        if (!now.isBefore(event.getStartTime())) {
            throw new ReservationUnavailableException(
                    "Registration is closed for this event.");
        }

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
                normalizedName,
                normalizedEmail,
                now.plus(HOLD_DURATION),
                normalizedKey,
                requestFingerprint);

        Reservation saved =
                reservationRepository.save(reservation);

        evictEventCache(eventId);
        outboxEventRecorder.recordHoldCreated(saved, now);

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
        outboxEventRecorder.recordConfirmed(reservation, OffsetDateTime.now());

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(
                        () -> new ReservationNotFoundException(reservationId));

        reservation.cancel();
        reservation.getEvent().releaseSpot();
        evictEventCache(reservation.getEvent().getId());
        outboxEventRecorder.recordCancelled(reservation, OffsetDateTime.now());

        return ReservationResponse.from(reservation);
    }

    private void evictEventCache(Long eventId) {
        try {
            eventCache.evict(eventId);
        } catch (RuntimeException exception) {
            // PostgreSQL has already applied the reservation change.
        }
    }
}
