package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.waitlist.WaitlistEntry;
import com.erfansadri.campusreserve.waitlist.WaitlistEntryRepository;
import com.erfansadri.campusreserve.waitlist.WaitlistStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "campusreserve.expiration.worker.enabled=false",
        "campusreserve.outbox.publisher.enabled=false"
})
class ReservationExpirationConcurrencyTests {

    @Autowired private ReservationExpirationProcessor expirationProcessor;
    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private WaitlistEntryRepository waitlistEntryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentWorkersExpireAndPromoteOnlyOnce() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = eventRepository.saveAndFlush(new Event(
                "Expiration concurrency", null, "UCSD", now.plusDays(1), now.minusDays(1), 1));
        event.reserveSpot();
        eventRepository.saveAndFlush(event);
        Reservation expiredHold = reservationRepository.saveAndFlush(new Reservation(
                event, "Held", "held@example.com", now.minusMinutes(1)));
        WaitlistEntry waiter = waitlistEntryRepository.saveAndFlush(new WaitlistEntry(
                event, "Waiter", "waiter@example.com"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return expirationProcessor.expireOverdueHolds(now);
            });
            Future<Integer> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return expirationProcessor.expireOverdueHolds(now);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(20, TimeUnit.SECONDS) + second.get(20, TimeUnit.SECONDS))
                    .isEqualTo(1);
            assertThat(reservationRepository.findById(expiredHold.getId()).orElseThrow().getStatus())
                    .isEqualTo(ReservationStatus.EXPIRED);
            assertThat(waitlistEntryRepository.findById(waiter.getId()).orElseThrow().getStatus())
                    .isEqualTo(WaitlistStatus.PROMOTED);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT remaining_capacity FROM events WHERE id = ?", Integer.class, event.getId()))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM reservations
                    WHERE event_id = ? AND status = 'HELD'
                    """, Integer.class, event.getId())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id IN (SELECT id FROM reservations WHERE event_id = ?)", event.getId());
            jdbcTemplate.update("DELETE FROM waitlist_entries WHERE event_id = ?", event.getId());
            jdbcTemplate.update("DELETE FROM reservations WHERE event_id = ?", event.getId());
            jdbcTemplate.update("DELETE FROM events WHERE id = ?", event.getId());
        }
    }

    @Test
    void cancellationPromotesOldestWaiterAndWritesBothOutboxEvents() {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = eventRepository.saveAndFlush(new Event(
                "Cancellation promotion", null, "UCSD", now.plusDays(1), now.minusDays(1), 1));
        event.reserveSpot();
        eventRepository.saveAndFlush(event);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                event, "Held", "held@example.com", now.plusMinutes(5)));
        WaitlistEntry first = waitlistEntryRepository.saveAndFlush(new WaitlistEntry(
                event, "First", "first@example.com"));
        WaitlistEntry second = waitlistEntryRepository.saveAndFlush(new WaitlistEntry(
                event, "Second", "second@example.com"));

        try {
            reservationService.cancelReservation(reservation.getId());

            assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                    .isEqualTo(ReservationStatus.CANCELLED);
            WaitlistEntry promoted = waitlistEntryRepository.findById(first.getId()).orElseThrow();
            assertThat(promoted.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
            assertThat(reservationRepository.findById(
                    promoted.getPromotedReservation().getId()).orElseThrow().getStatus())
                    .isEqualTo(ReservationStatus.HELD);
            assertThat(waitlistEntryRepository.findById(second.getId()).orElseThrow().getStatus())
                    .isEqualTo(WaitlistStatus.WAITING);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT remaining_capacity FROM events WHERE id = ?", Integer.class, event.getId()))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM outbox_events
                    WHERE aggregate_id IN (SELECT id FROM reservations WHERE event_id = ?)
                      AND event_type IN ('reservation.cancelled', 'reservation.hold.created')
                    """, Integer.class, event.getId())).isEqualTo(2);
        } finally {
            deleteEventData(event.getId());
        }
    }

    @Test
    void cancellationAndExpirationRaceReleaseAndPromoteOnlyOnce() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = eventRepository.saveAndFlush(new Event(
                "Cancellation expiration race", null, "UCSD", now.plusDays(1), now.minusDays(1), 1));
        event.reserveSpot();
        eventRepository.saveAndFlush(event);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                event, "Held", "held@example.com", now.minusMinutes(1)));
        WaitlistEntry waiter = waitlistEntryRepository.saveAndFlush(new WaitlistEntry(
                event, "Waiter", "waiter@example.com"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> cancellation = executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    reservationService.cancelReservation(reservation.getId());
                    return true;
                } catch (InvalidReservationStateException exception) {
                    return false;
                }
            });
            Future<Integer> expiration = executor.submit(() -> {
                ready.countDown();
                start.await();
                return expirationProcessor.expireOverdueHolds(now);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            cancellation.get(20, TimeUnit.SECONDS);
            expiration.get(20, TimeUnit.SECONDS);

            ReservationStatus finalStatus = reservationRepository.findById(reservation.getId())
                    .orElseThrow().getStatus();
            assertThat(finalStatus).isIn(ReservationStatus.CANCELLED, ReservationStatus.EXPIRED);
            assertThat(waitlistEntryRepository.findById(waiter.getId()).orElseThrow().getStatus())
                    .isEqualTo(WaitlistStatus.PROMOTED);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT remaining_capacity FROM events WHERE id = ?", Integer.class, event.getId()))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM reservations
                    WHERE event_id = ? AND status = 'HELD'
                    """, Integer.class, event.getId())).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            deleteEventData(event.getId());
        }
    }

    @Test
    void cancellationWithoutWaiterLeavesCapacityAvailable() {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = eventRepository.saveAndFlush(new Event(
                "Cancellation available capacity", null, "UCSD", now.plusDays(1), now.minusDays(1), 1));
        event.reserveSpot();
        eventRepository.saveAndFlush(event);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                event, "Held", "held@example.com", now.plusMinutes(5)));

        try {
            reservationService.cancelReservation(reservation.getId());
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT remaining_capacity FROM events WHERE id = ?", Integer.class, event.getId()))
                    .isEqualTo(1);
        } finally {
            deleteEventData(event.getId());
        }
    }

    private void deleteEventData(Long eventId) {
        jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id IN (SELECT id FROM reservations WHERE event_id = ?)", eventId);
        jdbcTemplate.update("DELETE FROM waitlist_entries WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM reservations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
    }
}
