package com.erfansadri.campusreserve.observability;

import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.stereotype.Component;

@Component
public class CampusReserveObservations {

    private final ObservationRegistry observationRegistry;

    public CampusReserveObservations(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public void observeOutboxBatch(Runnable action) {
        Observation.createNotStarted(
                "campusreserve.outbox.publish.batch", observationRegistry).observe(action);
    }

    public <T> T observeExpirationRun(Supplier<T> action) {
        return Observation.createNotStarted(
                "campusreserve.expiration.run", observationRegistry).observe(action);
    }

}
