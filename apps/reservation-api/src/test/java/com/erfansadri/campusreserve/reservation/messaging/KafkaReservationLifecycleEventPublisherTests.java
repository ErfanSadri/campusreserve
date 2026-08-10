package com.erfansadri.campusreserve.reservation.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.erfansadri.campusreserve.reservation.ReservationStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaReservationLifecycleEventPublisherTests {

    @Mock
    private KafkaTemplate<String, ReservationLifecycleEvent> kafkaTemplate;

    @InjectMocks
    private KafkaReservationLifecycleEventPublisher publisher;

    @Test
    void publishesEventToVersionedLifecycleTopicUsingReservationIdAsKey() {
        ReservationLifecycleEvent event = new ReservationLifecycleEvent(
                UUID.randomUUID(),
                "reservation.hold.created",
                "v1",
                7L,
                19L,
                ReservationStatus.HELD,
                "student@example.com",
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"));

        when(kafkaTemplate.send(
                ReservationLifecycleTopic.NAME,
                "19",
                event)).thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event).join();

        verify(kafkaTemplate).send(
                ReservationLifecycleTopic.NAME,
                "19",
                event);
    }
}
