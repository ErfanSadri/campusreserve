package com.erfansadri.campusreserve.outbox;

import com.erfansadri.campusreserve.reservation.messaging.ReservationLifecycleEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Component;

@Component
public class OutboxEventCodec {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String serialize(ReservationLifecycleEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event.", exception);
        }
    }

    public ReservationLifecycleEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ReservationLifecycleEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize outbox event.", exception);
        }
    }
}
