package com.erfansadri.campusreserve.messaging;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedConsumerEventRepository
        extends JpaRepository<ProcessedConsumerEvent, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_consumer_events (consumer_name, outbox_event_id)
            VALUES (:consumerName, :outboxEventId)
            ON CONFLICT (consumer_name, outbox_event_id) DO NOTHING
            """, nativeQuery = true)
    int claimIfUnprocessed(
            @Param("consumerName") String consumerName,
            @Param("outboxEventId") UUID outboxEventId);

    boolean existsByConsumerNameAndOutboxEventId(
            String consumerName,
            UUID outboxEventId);
}
