package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventCache eventCache;

    public EventService(
            EventRepository eventRepository,
            EventCache eventCache) {
        this.eventRepository = eventRepository;
        this.eventCache = eventCache;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        if (!request.registrationOpensAt().isBefore(request.startTime())) {
            throw new InvalidEventException(
                "Registration must open before the event starts.");
        }

        Event event = new Event(
                request.title(),
                request.description(),
                request.location(),
                request.startTime(),
                request.registrationOpensAt(),
                request.capacity());

        Event saved = eventRepository.save(event);

        return EventResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventRepository
                .findByStartTimeAfterOrderByStartTimeAsc(OffsetDateTime.now())
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        Optional<EventResponse> cachedEvent = getCachedEvent(id);

        if (cachedEvent.isPresent()) {
            return cachedEvent.orElseThrow();
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        EventResponse response = EventResponse.from(event);
        cacheEvent(response);

        return response;
    }

    private Optional<EventResponse> getCachedEvent(Long id) {
        try {
            return eventCache.get(id);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private void cacheEvent(EventResponse event) {
        try {
            eventCache.put(event);
        } catch (RuntimeException exception) {
            // The database response is still valid when cache writes fail.
        }
    }
}
