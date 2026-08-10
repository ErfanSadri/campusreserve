package com.erfansadri.campusreserve.reservation.messaging;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaReservationLifecycleEventPublisher
        implements ReservationLifecycleEventPublisher {

    private final KafkaTemplate<String, ReservationLifecycleEvent>
            kafkaTemplate;

    public KafkaReservationLifecycleEventPublisher(
            KafkaTemplate<String, ReservationLifecycleEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> publish(ReservationLifecycleEvent event) {
        return kafkaTemplate.send(
                ReservationLifecycleTopic.NAME,
                event.reservationId().toString(),
                event)
                .thenApply(result -> null);
    }
}
