package com.erfansadri.campusreserve.outbox;

import java.time.OffsetDateTime;
import java.util.List;

import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEventPublisher;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.observability.CampusReserveObservations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "campusreserve.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventCodec outboxEventCodec;
    private final ReservationLifecycleEventPublisher eventPublisher;
    private final CampusReserveMetrics metrics;
    private final CampusReserveObservations observations;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxEventCodec outboxEventCodec,
            ReservationLifecycleEventPublisher eventPublisher,
            CampusReserveMetrics metrics,
            CampusReserveObservations observations) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventCodec = outboxEventCodec;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.observations = observations;
    }

    @Scheduled(fixedDelayString = "${campusreserve.outbox.publisher.fixed-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        observations.observeOutboxBatch(this::publishPendingEventsInternal);
    }

    private void publishPendingEventsInternal() {
        var batch = metrics.startOutboxBatch();
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository
                    .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

            for (OutboxEvent outboxEvent : pendingEvents) {
                try {
                    ReservationLifecycleEvent event = outboxEventCodec
                            .deserialize(outboxEvent.getPayload());
                    eventPublisher.publish(event).join();
                    outboxEvent.markPublished(OffsetDateTime.now());
                    metrics.outboxPublished();
                } catch (RuntimeException exception) {
                    // Leave this and later records pending for a future poll.
                    metrics.outboxPublicationFailed();
                    return;
                }
            }
        } finally {
            metrics.outboxBatchFinished(batch);
        }
    }
}
