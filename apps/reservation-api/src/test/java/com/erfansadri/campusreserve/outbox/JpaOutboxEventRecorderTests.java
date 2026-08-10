package com.erfansadri.campusreserve.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.reservation.Reservation;
import com.erfansadri.campusreserve.reservation.ReservationStatus;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleTopic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JpaOutboxEventRecorderTests {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void recordsAStableLifecycleEnvelopeAsOutboxPayload() {
        OutboxEventCodec codec = new OutboxEventCodec();
        JpaOutboxEventRecorder recorder = new JpaOutboxEventRecorder(
                outboxEventRepository,
                codec);

        Event event = new Event(
                "Outbox Event",
                null,
                "UCSD",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().minusDays(1),
                10);
        ReflectionTestUtils.setField(event, "id", 7L);

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(10));
        ReflectionTestUtils.setField(reservation, "id", 19L);

        OffsetDateTime occurredAt = OffsetDateTime.now();
        recorder.recordHoldCreated(reservation, occurredAt);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent outboxEvent = captor.getValue();
        ReservationLifecycleEvent lifecycleEvent = codec.deserialize(
                outboxEvent.getPayload());

        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("reservation");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(19L);
        assertThat(outboxEvent.getTopic())
                .isEqualTo(ReservationLifecycleTopic.NAME);
        assertThat(lifecycleEvent.outboxEventId()).isEqualTo(outboxEvent.getId());
        assertThat(lifecycleEvent.eventType())
                .isEqualTo("reservation.hold.created");
        assertThat(lifecycleEvent.reservationStatus())
                .isEqualTo(ReservationStatus.HELD);
        assertThat(lifecycleEvent.eventId()).isEqualTo(7L);
        assertThat(lifecycleEvent.reservationId()).isEqualTo(19L);
        assertThat(lifecycleEvent.occurredAt()).isEqualTo(occurredAt);
    }
}
