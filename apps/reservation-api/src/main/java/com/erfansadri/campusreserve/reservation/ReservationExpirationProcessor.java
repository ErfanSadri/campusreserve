package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;
import java.util.List;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventCache;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;
import com.erfansadri.campusreserve.waitlist.WaitlistEntry;
import com.erfansadri.campusreserve.waitlist.WaitlistEntryRepository;
import com.erfansadri.campusreserve.waitlist.WaitlistStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationExpirationProcessor {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.HELD, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final EventCache eventCache;
    private final OutboxEventRecorder outboxEventRecorder;
    private final int batchSize;

    public ReservationExpirationProcessor(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            WaitlistEntryRepository waitlistEntryRepository,
            EventCache eventCache,
            OutboxEventRecorder outboxEventRecorder,
            @Value("${campusreserve.expiration.worker.batch-size}") int batchSize) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.eventCache = eventCache;
        this.outboxEventRecorder = outboxEventRecorder;
        this.batchSize = batchSize;
    }

    @Transactional
    public int expireOverdueHolds(OffsetDateTime now) {
        List<Reservation> overdueHolds = reservationRepository
                .findOverdueHoldsForUpdateSkipLocked(
                        ReservationStatus.HELD, now, PageRequest.of(0, batchSize));

        int expiredCount = 0;
        for (Reservation reservation : overdueHolds) {
            if (reservation.getStatus() != ReservationStatus.HELD
                    || reservation.getHeldUntil() == null
                    || reservation.getHeldUntil().isAfter(now)) {
                continue;
            }

            Event event = eventRepository.findByIdForUpdate(reservation.getEvent().getId())
                    .orElseThrow();
            reservation.expire();
            expiredCount++;
            event.releaseSpot();
            outboxEventRecorder.recordExpired(reservation, now);
            promoteOldestEligibleWaiter(event, now);
            evictEventCache(event.getId());
        }

        return expiredCount;
    }

    private void promoteOldestEligibleWaiter(Event event, OffsetDateTime now) {
        List<WaitlistEntry> waitingEntries = waitlistEntryRepository
                .findByEvent_IdAndStatusOrderByCreatedAtAscIdAsc(
                        event.getId(), WaitlistStatus.WAITING);

        for (WaitlistEntry entry : waitingEntries) {
            boolean alreadyReserved = reservationRepository
                    .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                            event.getId(), entry.getAttendeeEmail(), ACTIVE_STATUSES)
                    .isPresent();
            if (alreadyReserved) {
                continue;
            }

            if (!event.hasAvailableCapacity()) {
                return;
            }
            event.reserveSpot();
            Reservation promotedReservation = reservationRepository.save(new Reservation(
                    event,
                    entry.getAttendeeName(),
                    entry.getAttendeeEmail(),
                    now.plus(ReservationService.HOLD_DURATION)));
            entry.promote(promotedReservation, now);
            outboxEventRecorder.recordHoldCreated(promotedReservation, now);
            return;
        }
    }

    private void evictEventCache(Long eventId) {
        try {
            eventCache.evict(eventId);
        } catch (RuntimeException exception) {
            // PostgreSQL has already applied the reservation change.
        }
    }
}
