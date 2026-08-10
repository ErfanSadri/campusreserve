package com.erfansadri.campusreserve.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_consumer_events")
public class ProcessedConsumerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "outbox_event_id", nullable = false)
    private UUID outboxEventId;

    @Generated
    @Column(
            name = "processed_at",
            nullable = false,
            insertable = false,
            updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedConsumerEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public UUID getOutboxEventId() {
        return outboxEventId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
