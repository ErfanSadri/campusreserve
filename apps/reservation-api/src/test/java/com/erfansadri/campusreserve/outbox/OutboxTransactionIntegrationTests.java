package com.erfansadri.campusreserve.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.reservation.CreateReservationRequest;
import com.erfansadri.campusreserve.reservation.ReservationRepository;
import com.erfansadri.campusreserve.reservation.ReservationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "campusreserve.outbox.publisher.enabled=false")
class OutboxTransactionIntegrationTests {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventCodec outboxEventCodec;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> eventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Long eventId : eventIds) {
            jdbcTemplate.update(
                    "DELETE FROM outbox_events WHERE aggregate_id IN (SELECT id FROM reservations WHERE event_id = ?)",
                    eventId);
            jdbcTemplate.update("DELETE FROM reservations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
        }
    }

    @Test
    void holdCreationWritesOneOutboxEventAndReplayWritesNoSecondEvent() {
        Event event = saveOpenEvent();
        CreateReservationRequest request = new CreateReservationRequest(
                "Test Student",
                "student@example.com");

        reservationService.createHold(event.getId(), "outbox-hold", request);
        reservationService.createHold(event.getId(), "outbox-hold", request);

        List<OutboxEvent> events = eventsFor(event.getId());

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType())
                .isEqualTo("reservation.hold.created");
        assertThat(events.getFirst().getPublishedAt()).isNull();
        assertThat(outboxEventCodec.deserialize(events.getFirst().getPayload())
                .outboxEventId()).isEqualTo(events.getFirst().getId());
    }

    @Test
    void confirmationWritesConfirmationOutboxEvent() {
        Event event = saveOpenEvent();
        Long reservationId = reservationIdForHold(event.getId(), "outbox-confirm");

        reservationService.confirmReservation(reservationId);

        assertThat(eventTypesFor(event.getId()))
                .containsExactlyInAnyOrder(
                        "reservation.hold.created",
                        "reservation.confirmed");
    }

    @Test
    void cancellationWritesCancellationOutboxEvent() {
        Event event = saveOpenEvent();
        Long reservationId = reservationIdForHold(event.getId(), "outbox-cancel");

        reservationService.cancelReservation(reservationId);

        assertThat(eventTypesFor(event.getId()))
                .containsExactlyInAnyOrder(
                        "reservation.hold.created",
                        "reservation.cancelled");
    }

    @Test
    void reservationMutationAndOutboxInsertRollBackTogether() {
        Event event = saveOpenEvent();

        transactionTemplate.executeWithoutResult(status -> {
            reservationService.createHold(
                    event.getId(),
                    "outbox-rollback",
                    new CreateReservationRequest(
                            "Test Student",
                            "student@example.com"));

            assertThat(eventsFor(event.getId())).hasSize(1);
            status.setRollbackOnly();
        });

        Integer reservationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE event_id = ?",
                Integer.class,
                event.getId());

        assertThat(reservationCount).isZero();
        assertThat(eventsFor(event.getId())).isEmpty();
        assertThat(eventRepository.findById(event.getId()).orElseThrow()
                .getRemainingCapacity()).isEqualTo(event.getCapacity());
    }

    private Event saveOpenEvent() {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = eventRepository.saveAndFlush(new Event(
                "Outbox Test Event",
                null,
                "UCSD",
                now.plusDays(3),
                now.minusDays(1),
                10));
        eventIds.add(event.getId());
        return event;
    }

    private Long reservationIdForHold(Long eventId, String key) {
        reservationService.createHold(
                eventId,
                key,
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com"));

        return reservationRepository
                .findByEvent_IdAndIdempotencyKey(eventId, key)
                .orElseThrow()
                .getId();
    }

    private List<OutboxEvent> eventsFor(Long eventId) {
        List<Long> reservationIds = reservationRepository.findByEvent_Id(eventId)
                .stream()
                .map(reservation -> reservation.getId())
                .toList();

        if (reservationIds.isEmpty()) {
            return List.of();
        }

        return outboxEventRepository.findByAggregateIdIn(reservationIds);
    }

    private List<String> eventTypesFor(Long eventId) {
        return eventsFor(eventId).stream()
                .map(OutboxEvent::getEventType)
                .toList();
    }
}
