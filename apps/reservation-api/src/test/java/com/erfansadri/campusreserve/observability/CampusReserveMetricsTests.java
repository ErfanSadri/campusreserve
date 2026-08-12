package com.erfansadri.campusreserve.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

class CampusReserveMetricsTests {

    @Test
    void recordsReservationAndMessagingMetricsWithStableNames() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CampusReserveMetrics metrics = new CampusReserveMetrics(registry);

        var hold = metrics.startHoldCreation();
        metrics.holdCreated(hold);
        metrics.reservationConfirmed();
        metrics.reservationCancelled();
        metrics.reservationExpired();
        metrics.waitlistEntryCreated();
        metrics.waitlistPromoted();
        metrics.outboxPublished();
        metrics.outboxPublicationFailed();
        metrics.kafkaProcessed();
        metrics.kafkaDuplicateSkipped();
        metrics.kafkaDltArrived();

        assertThat(registry.get("campusreserve.reservations.holds.created").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("campusreserve.outbox.events.published").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("campusreserve.kafka.lifecycle.dlt.arrivals").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("campusreserve.reservations.hold.creation").timer().count())
                .isEqualTo(1);
    }

    @Test
    void recordsOutboxAndExpirationTimingAndCount() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CampusReserveMetrics metrics = new CampusReserveMetrics(registry);

        var batch = metrics.startOutboxBatch();
        metrics.outboxBatchFinished(batch);
        var expiration = metrics.startExpirationRun();
        metrics.expirationRunFinished(expiration, 3);

        assertThat(registry.get("campusreserve.outbox.publish.batch").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("campusreserve.expiration.run").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("campusreserve.expiration.processed").summary().totalAmount())
                .isEqualTo(3);
    }
}
