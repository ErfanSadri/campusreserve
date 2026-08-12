package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "campusreserve.expiration.worker.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReservationExpirationWorker {

    private final ReservationExpirationProcessor expirationProcessor;

    public ReservationExpirationWorker(ReservationExpirationProcessor expirationProcessor) {
        this.expirationProcessor = expirationProcessor;
    }

    @Scheduled(fixedDelayString = "${campusreserve.expiration.worker.fixed-delay-ms}")
    public void expireOverdueHolds() {
        expirationProcessor.expireOverdueHolds(OffsetDateTime.now());
    }
}
