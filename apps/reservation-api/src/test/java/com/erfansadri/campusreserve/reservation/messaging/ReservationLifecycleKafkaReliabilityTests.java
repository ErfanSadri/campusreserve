package com.erfansadri.campusreserve.reservation.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.erfansadri.campusreserve.reservation.ReservationStatus;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReservationLifecycleKafkaReliabilityTests {

    @Test
    void retriesTwiceThenPublishesFailedRecordToDltWithOriginalMetadata() {
        KafkaTemplate<String, ReservationLifecycleEvent> kafkaTemplate = mock(
                KafkaTemplate.class);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        DefaultErrorHandler handler = new ReservationKafkaConfiguration()
                .reservationLifecycleErrorHandler(kafkaTemplate);
        ConsumerRecord<String, ReservationLifecycleEvent> record = new ConsumerRecord<>(
                ReservationLifecycleTopic.NAME,
                0,
                12L,
                "reservation-19",
                event());
        record.headers().add("source", "test".getBytes(StandardCharsets.UTF_8));

        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        RuntimeException failure = new RuntimeException("transient failure");

        assertThat(handler.handleOne(failure, record, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, record, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, record, consumer, container)).isTrue();

        ArgumentCaptor<ProducerRecord<String, ReservationLifecycleEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, ReservationLifecycleEvent> dltRecord = captor.getValue();
        assertThat(dltRecord.topic()).isEqualTo(ReservationLifecycleTopic.DLT_NAME);
        assertThat(dltRecord.value().outboxEventId())
                .isEqualTo(record.value().outboxEventId());
        assertThat(dltRecord.headers().lastHeader("source")).isNotNull();
        assertThat(dltRecord.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC))
                .isNotNull();
        assertThat(dltRecord.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN))
                .isNotNull();
    }

    @Test
    void configuresVersionedLifecycleAndDltTopics() {
        ReservationKafkaConfiguration configuration =
                new ReservationKafkaConfiguration();

        assertThat(configuration.reservationLifecycleTopic().name())
                .isEqualTo(ReservationLifecycleTopic.NAME);
        assertThat(configuration.reservationLifecycleDltTopic().name())
                .isEqualTo(ReservationLifecycleTopic.DLT_NAME);
    }

    private ReservationLifecycleEvent event() {
        return new ReservationLifecycleEvent(
                UUID.randomUUID(),
                "reservation.hold.created",
                "v1",
                7L,
                19L,
                ReservationStatus.HELD,
                "student@example.com",
                OffsetDateTime.parse("2026-08-09T15:03:41Z"));
    }
}
