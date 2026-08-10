package com.erfansadri.campusreserve.reservation.messaging;

import java.util.concurrent.CompletableFuture;

public interface ReservationLifecycleEventPublisher {

    CompletableFuture<Void> publish(ReservationLifecycleEvent event);
}
