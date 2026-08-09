package com.erfansadri.campusreserve.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class EventRepositoryTests {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void savesAndLoadsEvent() {
        OffsetDateTime now = OffsetDateTime.now();

        Event event = new Event(
                "Software Engineering Interview Workshop",
                "Practice technical interviews with other students.",
                "UCSD Price Center",
                now.plusDays(10),
                now.plusDays(2),
                60);

        Event saved = eventRepository.saveAndFlush(event);

        Event loaded = eventRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getTitle())
                .isEqualTo("Software Engineering Interview Workshop");
        assertThat(loaded.getCapacity()).isEqualTo(60);
        assertThat(loaded.getRemainingCapacity()).isEqualTo(60);
    }

    @Test
    void returnsUpcomingEventsInStartTimeOrder() {
        OffsetDateTime now = OffsetDateTime.now();

        Event later = new Event(
                "Later Event",
                null,
                "UCSD",
                now.plusDays(20),
                now.plusDays(1),
                50);

        Event sooner = new Event(
                "Sooner Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.plusDays(1),
                50);

        eventRepository.save(later);
        eventRepository.save(sooner);
        eventRepository.flush();

        List<Event> events =
                eventRepository.findByStartTimeAfterOrderByStartTimeAsc(now);

        assertThat(events)
                .extracting(Event::getTitle)
                .containsSubsequence("Sooner Event", "Later Event");
    }
}