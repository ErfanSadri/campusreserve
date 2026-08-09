package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartTimeAfterOrderByStartTimeAsc(OffsetDateTime startTime);
}