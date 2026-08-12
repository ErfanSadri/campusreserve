package com.erfansadri.campusreserve.reservation.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.erfansadri.campusreserve.messaging.IdempotentConsumerProcessor;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.reservation.ReservationStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationLifecycleAuditConsumerTests {

    @Mock private IdempotentConsumerProcessor idempotentConsumerProcessor;
    @Mock private CampusReserveMetrics metrics;

    @Test
    void distinguishesFirstProcessingFromAnIdempotentDuplicate() {
        ReservationLifecycleAuditConsumer consumer = new ReservationLifecycleAuditConsumer(
                idempotentConsumerProcessor, metrics);
        ReservationLifecycleEvent event = new ReservationLifecycleEvent(
                UUID.randomUUID(), "reservation.hold.created", "v1", 7L, 19L,
                ReservationStatus.HELD, "student@example.com", OffsetDateTime.now());

        when(idempotentConsumerProcessor.processOnce(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true, false);

        consumer.consume(event);
        consumer.consume(event);

        verify(metrics).kafkaProcessed();
        verify(metrics).kafkaDuplicateSkipped();
    }
}
