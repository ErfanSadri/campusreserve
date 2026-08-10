package com.erfansadri.campusreserve.reservation.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.erfansadri.campusreserve.messaging.IdempotentConsumerProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

@Service
public class ReservationLifecycleAuditConsumer {

    public static final String CONSUMER_NAME =
            "reservation-lifecycle-audit-v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ReservationLifecycleAuditConsumer.class);

    private final IdempotentConsumerProcessor idempotentConsumerProcessor;

    public ReservationLifecycleAuditConsumer(
            IdempotentConsumerProcessor idempotentConsumerProcessor) {
        this.idempotentConsumerProcessor = idempotentConsumerProcessor;
    }

    @KafkaListener(topics = ReservationLifecycleTopic.NAME)
    public void consume(ReservationLifecycleEvent event) {
        idempotentConsumerProcessor.processOnce(
                CONSUMER_NAME,
                event.outboxEventId(),
                () -> LOGGER.info(
                        "Received reservation lifecycle event outboxEventId={} type={} reservationId={} eventId={}",
                        event.outboxEventId(),
                        event.eventType(),
                        event.reservationId(),
                        event.eventId()));
    }

    @KafkaListener(
            topics = ReservationLifecycleTopic.DLT_NAME,
            groupId = "campusreserve-reservation-audit-dlt-v1")
    public void consumeDeadLetter(
            ConsumerRecord<String, ReservationLifecycleEvent> record) {
        ReservationLifecycleEvent event = record.value();
        var originalTopicHeader = record.headers().lastHeader(
                KafkaHeaders.DLT_ORIGINAL_TOPIC);
        String originalTopic = originalTopicHeader == null
                ? "unknown"
                : new String(
                        originalTopicHeader.value(),
                        StandardCharsets.UTF_8);
        LOGGER.warn(
                "Received reservation lifecycle DLT event outboxEventId={} type={} reservationId={} originalTopic={} headers={}",
                event.outboxEventId(),
                event.eventType(),
                event.reservationId(),
                originalTopic,
                record.headers().toArray().length);
    }
}
