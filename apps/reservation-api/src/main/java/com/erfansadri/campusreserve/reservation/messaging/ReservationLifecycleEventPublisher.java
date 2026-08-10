package com.erfansadri.campusreserve.reservation.messaging;

public interface ReservationLifecycleEventPublisher {

    void publish(ReservationLifecycleEvent event);
}
