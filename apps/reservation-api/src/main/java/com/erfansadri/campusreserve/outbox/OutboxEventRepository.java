package com.erfansadri.campusreserve.outbox;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    List<OutboxEvent> findByAggregateIdIn(Collection<Long> aggregateIds);
}
