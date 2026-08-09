package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ReservationRepositoryTests {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void savesAndLoadsReservation() {
        OffsetDateTime now =
                OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        Event event = eventRepository.saveAndFlush(new Event(
                "Backend Engineering Workshop",
                "Learn about backend systems.",
                "UCSD Price Center",
                now.plusDays(10),
                now.plusDays(1),
                50));

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                now.plusMinutes(10));

        Reservation saved =
                reservationRepository.saveAndFlush(reservation);

        Reservation loaded = reservationRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(loaded.getEvent().getId()).isEqualTo(event.getId());
        assertThat(loaded.getAttendeeName()).isEqualTo("Test Student");
        assertThat(loaded.getAttendeeEmail()).isEqualTo("student@example.com");
        assertThat(loaded.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(loaded.getHeldUntil()).isEqualTo(now.plusMinutes(10));
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsActiveReservationIgnoringEmailCase() {
        OffsetDateTime now =
                OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        Event event = eventRepository.saveAndFlush(new Event(
                "Systems Workshop",
                null,
                "UCSD",
                now.plusDays(10),
                now.plusDays(1),
                25));

        reservationRepository.saveAndFlush(new Reservation(
                event,
                "Test Student",
                "Student@Example.com",
                now.plusMinutes(10)));

        var result =
                reservationRepository
                        .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                                event.getId(),
                                "student@example.com",
                                List.of(
                                        ReservationStatus.HELD,
                                        ReservationStatus.CONFIRMED));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void preventsDuplicateActiveReservationForSameEventAndEmail() {
        OffsetDateTime now =
                OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        Event event = eventRepository.saveAndFlush(new Event(
                "Distributed Systems Workshop",
                null,
                "UCSD",
                now.plusDays(10),
                now.plusDays(1),
                30));

        reservationRepository.saveAndFlush(new Reservation(
                event,
                "First Student",
                "student@example.com",
                now.plusMinutes(10)));

        Reservation duplicate = new Reservation(
                event,
                "Same Student",
                "STUDENT@example.com",
                now.plusMinutes(10));

        assertThatThrownBy(
                () -> reservationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}