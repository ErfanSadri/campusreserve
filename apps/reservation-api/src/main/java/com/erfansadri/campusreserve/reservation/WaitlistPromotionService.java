package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;
import java.util.List;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;
import com.erfansadri.campusreserve.waitlist.WaitlistEntry;
import com.erfansadri.campusreserve.waitlist.WaitlistEntryRepository;
import com.erfansadri.campusreserve.waitlist.WaitlistStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promotes at most one waiter while the caller holds the event's PostgreSQL row lock.
 */
@Service
public class WaitlistPromotionService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.HELD, ReservationStatus.CONFIRMED);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxEventRecorder outboxEventRecorder;

    public WaitlistPromotionService(
            WaitlistEntryRepository waitlistEntryRepository,
            ReservationRepository reservationRepository,
            OutboxEventRecorder outboxEventRecorder) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.reservationRepository = reservationRepository;
        this.outboxEventRecorder = outboxEventRecorder;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void promoteOldestEligibleWaiter(Event event, OffsetDateTime now) {
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
}
