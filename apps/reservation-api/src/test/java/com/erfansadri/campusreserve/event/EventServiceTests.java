package com.erfansadri.campusreserve.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTests {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createsEventWithFullRemainingCapacity() {
        OffsetDateTime registrationOpensAt =
                OffsetDateTime.parse("2026-09-20T09:00:00-07:00");
        OffsetDateTime startTime =
                OffsetDateTime.parse("2026-09-28T18:00:00-07:00");

        CreateEventRequest request = new CreateEventRequest(
                "Software Engineering Interview Workshop",
                "Practice technical interviews with other students.",
                "UCSD Price Center",
                startTime,
                registrationOpensAt,
                60);

        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.title())
                .isEqualTo("Software Engineering Interview Workshop");
        assertThat(response.capacity()).isEqualTo(60);
        assertThat(response.remainingCapacity()).isEqualTo(60);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void rejectsEventWhenRegistrationDoesNotOpenBeforeStart() {
        OffsetDateTime startTime =
                OffsetDateTime.parse("2026-09-28T18:00:00-07:00");

        CreateEventRequest request = new CreateEventRequest(
                "Invalid Event",
                null,
                "UCSD",
                startTime,
                startTime,
                50);

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(InvalidEventException.class)
                .hasMessage("Registration must open before the event starts.");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void returnsUpcomingEvents() {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-09-01T12:00:00-07:00");

        Event sooner = new Event(
                "Sooner Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.plusDays(1),
                50);

        Event later = new Event(
                "Later Event",
                null,
                "UCSD",
                now.plusDays(20),
                now.plusDays(1),
                50);

        when(eventRepository.findByStartTimeAfterOrderByStartTimeAsc(
                any(OffsetDateTime.class)))
                .thenReturn(List.of(sooner, later));

        List<EventResponse> responses = eventService.getUpcomingEvents();

        assertThat(responses)
                .extracting(EventResponse::title)
                .containsExactly("Sooner Event", "Later Event");

        verify(eventRepository)
                .findByStartTimeAfterOrderByStartTimeAsc(
                        any(OffsetDateTime.class));
    }

    @Test
    void throwsWhenEventDoesNotExist() {
        when(eventRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Event 999 was not found.");
    }
}