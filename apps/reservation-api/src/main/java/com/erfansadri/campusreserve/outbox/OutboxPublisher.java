package com.erfansadri.campusreserve.outbox;

import java.time.OffsetDateTime;
import java.util.List;

import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEventPublisher;

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

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxEventCodec outboxEventCodec,
            ReservationLifecycleEventPublisher eventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventCodec = outboxEventCodec;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${campusreserve.outbox.publisher.fixed-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                ReservationLifecycleEvent event = outboxEventCodec
                        .deserialize(outboxEvent.getPayload());
                eventPublisher.publish(event).join();
                outboxEvent.markPublished(OffsetDateTime.now());
            } catch (RuntimeException exception) {
                // Leave this and later records pending for a future poll.
                return;
            }
        }
    }
}
