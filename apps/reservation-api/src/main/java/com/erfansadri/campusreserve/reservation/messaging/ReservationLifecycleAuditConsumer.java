package com.erfansadri.campusreserve.reservation.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReservationLifecycleAuditConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ReservationLifecycleAuditConsumer.class);

    @KafkaListener(topics = ReservationLifecycleTopic.NAME)
    public void consume(ReservationLifecycleEvent event) {
        LOGGER.info(
                "Received reservation lifecycle event outboxEventId={} type={} reservationId={} eventId={}",
                event.outboxEventId(),
                event.eventType(),
                event.reservationId(),
                event.eventId());
    }
}
