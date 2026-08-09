package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        return EventResponse.from(event);
    }
}