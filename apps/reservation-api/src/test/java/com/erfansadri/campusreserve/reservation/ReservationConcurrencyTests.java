package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ReservationConcurrencyTests {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void neverOverbooksEventUnderConcurrentReservationAttempts()
            throws Exception {

        OffsetDateTime now = OffsetDateTime.now();

        Event event = eventRepository.saveAndFlush(new Event(
                "Concurrency Test Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.minusDays(1),
                1));

        Long eventId = event.getId();

        int attempts = 40;

        ExecutorService executor =
                Executors.newFixedThreadPool(attempts);

        CountDownLatch ready =
                new CountDownLatch(attempts);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                int studentNumber = i;

                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    try {
                        reservationService.createHold(
                            eventId,
                            "concurrency-" + studentNumber,
                            new CreateReservationRequest(
                                    "Student " + studentNumber,
                                    "student" + studentNumber + "@example.com"));

                        return true;
                    } catch (ReservationUnavailableException exception) {
                        return false;
                    }
                }));
            }

            assertThat(
                    ready.await(5, TimeUnit.SECONDS))
                    .isTrue();

            start.countDown();

            int successfulReservations = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulReservations++;
                }
            }

            Event reloaded = eventRepository
                    .findById(eventId)
                    .orElseThrow();

            Integer activeReservations =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM reservations
                            WHERE event_id = ?
                              AND status IN ('HELD', 'CONFIRMED')
                            """,
                            Integer.class,
                            eventId);

            assertThat(successfulReservations).isEqualTo(1);
            assertThat(activeReservations).isEqualTo(1);
            assertThat(reloaded.getRemainingCapacity()).isZero();

        } finally {
            executor.shutdownNow();

            jdbcTemplate.update(
                    "DELETE FROM reservations WHERE event_id = ?",
                    eventId);

            jdbcTemplate.update(
                    "DELETE FROM events WHERE id = ?",
                    eventId);
        }
    }

    @Test
    void honorsCapacityUnderHighConcurrentLoad() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();

        int capacity = 100;
        int attempts = 500;
        int workers = 50;

        Event event = eventRepository.saveAndFlush(new Event(
                "High Load Concurrency Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.minusDays(1),
                capacity));

        Long eventId = event.getId();

        ExecutorService executor =
                Executors.newFixedThreadPool(workers);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Boolean>> futures =
                new ArrayList<>(attempts);

        try {
            for (int i = 0; i < attempts; i++) {
                int studentNumber = i;

                futures.add(executor.submit(() -> {
                    start.await();

                    try {
                        reservationService.createHold(
                            eventId,
                            "load-" + studentNumber,
                            new CreateReservationRequest(
                                    "Load Student " + studentNumber,
                                    "load-student" + studentNumber + "@example.com"));

                        return true;
                    } catch (ReservationUnavailableException exception) {
                        return false;
                    }
                }));
            }

            start.countDown();

            int successfulReservations = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(60, TimeUnit.SECONDS)) {
                    successfulReservations++;
                }
            }

            Event reloaded = eventRepository
                    .findById(eventId)
                    .orElseThrow();

            Integer activeReservations =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM reservations
                            WHERE event_id = ?
                            AND status IN ('HELD', 'CONFIRMED')
                            """,
                            Integer.class,
                            eventId);

            assertThat(successfulReservations)
                    .isEqualTo(capacity);

            assertThat(activeReservations)
                    .isEqualTo(capacity);

            assertThat(reloaded.getRemainingCapacity())
                    .isZero();

        } finally {
            executor.shutdownNow();

            jdbcTemplate.update(
                    "DELETE FROM reservations WHERE event_id = ?",
                    eventId);

            jdbcTemplate.update(
                    "DELETE FROM events WHERE id = ?",
                    eventId);
        }
    }

    @Test
    void replaysConcurrentRequestsWithTheSameKeyWithoutUsingExtraCapacity()
            throws Exception {
        OffsetDateTime now = OffsetDateTime.now();

        Event event = eventRepository.saveAndFlush(new Event(
                "Idempotent Concurrency Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.minusDays(1),
                1));

        Long eventId = event.getId();
        int attempts = 20;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ReservationResponse>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return reservationService.createHold(
                            eventId,
                            "shared-request-key",
                            new CreateReservationRequest(
                                    "Test Student",
                                    "student@example.com"));
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Long> reservationIds = new ArrayList<>();
            for (Future<ReservationResponse> future : futures) {
                reservationIds.add(future.get(30, TimeUnit.SECONDS).id());
            }

            Event reloaded = eventRepository.findById(eventId).orElseThrow();
            Integer activeReservations = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM reservations
                    WHERE event_id = ?
                      AND status IN ('HELD', 'CONFIRMED')
                    """,
                    Integer.class,
                    eventId);

            assertThat(reservationIds).hasSize(attempts).containsOnly(reservationIds.getFirst());
            assertThat(activeReservations).isEqualTo(1);
            assertThat(reloaded.getRemainingCapacity()).isZero();
        } finally {
            executor.shutdownNow();
            jdbcTemplate.update("DELETE FROM reservations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
        }
    }
}
