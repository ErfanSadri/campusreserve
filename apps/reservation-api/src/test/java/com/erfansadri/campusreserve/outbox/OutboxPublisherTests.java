package com.erfansadri.campusreserve.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.erfansadri.campusreserve.reservation.ReservationStatus;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEventPublisher;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleTopic;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.observability.CampusReserveObservations;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTests {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ReservationLifecycleEventPublisher eventPublisher;

    @Mock
    private CampusReserveMetrics metrics;

    @Test
    void publishesPendingEventAndMarksItPublishedAfterAcknowledgement() {
        OutboxEventCodec codec = new OutboxEventCodec();
        ReservationLifecycleEvent event = lifecycleEvent();
        OutboxEvent outboxEvent = outboxEvent(event, codec);
        OutboxPublisher publisher = new OutboxPublisher(
                outboxEventRepository,
                codec,
                eventPublisher,
                metrics,
                new CampusReserveObservations(ObservationRegistry.NOOP));

        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(eventPublisher.publish(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        ArgumentCaptor<ReservationLifecycleEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservationLifecycleEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().outboxEventId())
                .isEqualTo(event.outboxEventId());
        assertThat(eventCaptor.getValue().occurredAt().toInstant())
                .isEqualTo(event.occurredAt().toInstant());
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
        verify(metrics).outboxPublished();
    }

    @Test
    void leavesPendingEventUnpublishedWhenKafkaPublishingFails() {
        OutboxEventCodec codec = new OutboxEventCodec();
        ReservationLifecycleEvent event = lifecycleEvent();
        OutboxEvent outboxEvent = outboxEvent(event, codec);
        OutboxPublisher publisher = new OutboxPublisher(
                outboxEventRepository,
                codec,
                eventPublisher,
                metrics,
                new CampusReserveObservations(ObservationRegistry.NOOP));

        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(eventPublisher.publish(any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new IllegalStateException("Kafka unavailable")));

        publisher.publishPendingEvents();

        assertThat(outboxEvent.getPublishedAt()).isNull();
        verify(metrics).outboxPublicationFailed();
    }

    @Test
    void doesNotRepublishAlreadyPublishedEvents() {
        OutboxPublisher publisher = new OutboxPublisher(
                outboxEventRepository,
                new OutboxEventCodec(),
                eventPublisher,
                metrics,
                new CampusReserveObservations(ObservationRegistry.NOOP));

        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(eventPublisher, never()).publish(any());
    }

    private ReservationLifecycleEvent lifecycleEvent() {
        return new ReservationLifecycleEvent(
                UUID.randomUUID(),
                "reservation.hold.created",
                "v1",
                7L,
                19L,
                ReservationStatus.HELD,
                "student@example.com",
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"));
    }

    private OutboxEvent outboxEvent(
            ReservationLifecycleEvent event,
            OutboxEventCodec codec) {
        return new OutboxEvent(
                event.outboxEventId(),
                "reservation",
                event.reservationId(),
                event.eventType(),
                event.eventVersion(),
                ReservationLifecycleTopic.NAME,
                codec.serialize(event),
                event.occurredAt());
    }
}
