package com.erfansadri.campusreserve.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "campusreserve.outbox.publisher.enabled=false")
class IdempotentConsumerProcessorIntegrationTests {

    @Autowired
    private IdempotentConsumerProcessor processor;

    @Autowired
    private ProcessedConsumerEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> eventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID eventId : eventIds) {
            jdbcTemplate.update(
                    "DELETE FROM processed_consumer_events WHERE outbox_event_id = ?",
                    eventId);
        }
    }

    @Test
    void processesFirstDeliveryAndSkipsDuplicateForTheSameConsumer() {
        UUID eventId = track(UUID.randomUUID());
        AtomicInteger sideEffects = new AtomicInteger();

        assertThat(processor.processOnce(
                "audit-v1",
                eventId,
                sideEffects::incrementAndGet)).isTrue();
        assertThat(processor.processOnce(
                "audit-v1",
                eventId,
                sideEffects::incrementAndGet)).isFalse();

        assertThat(sideEffects).hasValue(1);
        assertThat(repository.existsByConsumerNameAndOutboxEventId(
                "audit-v1", eventId)).isTrue();
    }

    @Test
    void allowsDifferentConsumersToProcessTheSameEventIndependently() {
        UUID eventId = track(UUID.randomUUID());
        AtomicInteger sideEffects = new AtomicInteger();

        processor.processOnce("audit-v1", eventId, sideEffects::incrementAndGet);
        processor.processOnce("analytics-v1", eventId, sideEffects::incrementAndGet);

        assertThat(sideEffects).hasValue(2);
        assertThat(repository.findAll().stream()
                .filter(processed -> processed.getOutboxEventId().equals(eventId)))
                .hasSize(2);
    }

    @Test
    void doesNotMarkFailedProcessingAsSuccessful() {
        UUID eventId = track(UUID.randomUUID());

        assertThatThrownBy(() -> processor.processOnce(
                "audit-v1",
                eventId,
                () -> {
                    throw new IllegalStateException("transient audit failure");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(repository.existsByConsumerNameAndOutboxEventId(
                "audit-v1", eventId)).isFalse();
    }

    private UUID track(UUID eventId) {
        eventIds.add(eventId);
        return eventId;
    }
}
