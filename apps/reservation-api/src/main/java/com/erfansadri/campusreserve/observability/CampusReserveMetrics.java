package com.erfansadri.campusreserve.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

@Component
public class CampusReserveMetrics {

    private final Counter holdsCreated;
    private final Counter reservationsConfirmed;
    private final Counter reservationsCancelled;
    private final Counter reservationsExpired;
    private final Counter waitlistEntriesCreated;
    private final Counter waitlistPromotions;
    private final Counter outboxPublished;
    private final Counter outboxPublicationFailures;
    private final Counter kafkaProcessed;
    private final Counter kafkaDuplicatesSkipped;
    private final Counter kafkaDltArrivals;
    private final Timer holdCreationTimer;
    private final DistributionSummary expirationProcessed;

    public CampusReserveMetrics(MeterRegistry registry) {
        holdsCreated = counter(registry, "campusreserve.reservations.holds.created");
        reservationsConfirmed = counter(registry, "campusreserve.reservations.confirmed");
        reservationsCancelled = counter(registry, "campusreserve.reservations.cancelled");
        reservationsExpired = counter(registry, "campusreserve.reservations.expired");
        waitlistEntriesCreated = counter(registry, "campusreserve.waitlist.entries.created");
        waitlistPromotions = counter(registry, "campusreserve.waitlist.promotions");
        outboxPublished = counter(registry, "campusreserve.outbox.events.published");
        outboxPublicationFailures = counter(registry, "campusreserve.outbox.publication.failures");
        kafkaProcessed = counter(registry, "campusreserve.kafka.lifecycle.processed");
        kafkaDuplicatesSkipped = counter(registry, "campusreserve.kafka.lifecycle.duplicates.skipped");
        kafkaDltArrivals = counter(registry, "campusreserve.kafka.lifecycle.dlt.arrivals");
        holdCreationTimer = Timer.builder("campusreserve.reservations.hold.creation")
                .description("Time spent creating reservation holds")
                .register(registry);
        expirationProcessed = DistributionSummary.builder("campusreserve.expiration.processed")
                .description("Holds expired in each expiration worker run")
                .register(registry);
    }

    public Timer.Sample startHoldCreation() { return Timer.start(); }
    public void holdCreated(Timer.Sample sample) { holdsCreated.increment(); sample.stop(holdCreationTimer); }
    public void reservationConfirmed() { reservationsConfirmed.increment(); }
    public void reservationCancelled() { reservationsCancelled.increment(); }
    public void reservationExpired() { reservationsExpired.increment(); }
    public void waitlistEntryCreated() { waitlistEntriesCreated.increment(); }
    public void waitlistPromoted() { waitlistPromotions.increment(); }
    public void outboxPublished() { outboxPublished.increment(); }
    public void outboxPublicationFailed() { outboxPublicationFailures.increment(); }
    public void kafkaProcessed() { kafkaProcessed.increment(); }
    public void kafkaDuplicateSkipped() { kafkaDuplicatesSkipped.increment(); }
    public void kafkaDltArrived() { kafkaDltArrivals.increment(); }
    public void expirationRunProcessed(int processed) {
        expirationProcessed.record(processed);
    }

    private Counter counter(MeterRegistry registry, String name) {
        return Counter.builder(name).register(registry);
    }
}
