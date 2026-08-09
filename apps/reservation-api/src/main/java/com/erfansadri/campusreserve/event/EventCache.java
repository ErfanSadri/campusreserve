package com.erfansadri.campusreserve.event;

import java.util.Optional;

public interface EventCache {

    Optional<EventResponse> get(Long eventId);

    void put(EventResponse event);

    void evict(Long eventId);
}
