package com.erfansadri.campusreserve.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.reservation.Reservation;
import com.erfansadri.campusreserve.reservation.ReservationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTests {

    @Mock private WaitlistEntryRepository waitlistEntryRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CampusReserveMetrics metrics;
    @InjectMocks private WaitlistService waitlistService;

    @Test
    void joinsAFullEventWithNormalizedAttendeeDetails() {
        Event event = openEvent(1);
        event.reserveSpot();
        when(eventRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(event));
        when(reservationRepository.findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                eq(3L), eq("student@example.com"), any())).thenReturn(Optional.empty());
        when(waitlistEntryRepository.existsByEvent_IdAndAttendeeEmailIgnoreCaseAndStatus(
                3L, "student@example.com", WaitlistStatus.WAITING)).thenReturn(false);
        when(waitlistEntryRepository.save(any(WaitlistEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WaitlistEntryResponse response = waitlistService.join(3L,
                new JoinWaitlistRequest("  Student  ", " Student@Example.com "));

        assertThat(response.attendeeName()).isEqualTo("Student");
        assertThat(response.attendeeEmail()).isEqualTo("student@example.com");
        assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING);
        verify(metrics).waitlistEntryCreated();
    }

    @Test
    void rejectsWaitlistWhenCapacityIsAvailable() {
        when(eventRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(openEvent(1)));

        assertThatThrownBy(() -> waitlistService.join(3L,
                new JoinWaitlistRequest("Student", "student@example.com")))
                .isInstanceOf(WaitlistUnavailableException.class)
                .hasMessageContaining("available capacity");
        verify(waitlistEntryRepository, never()).save(any());
    }

    @Test
    void rejectsAnAttendeeWithAnActiveReservationOrExistingWaitlistEntry() {
        Event event = openEvent(1);
        event.reserveSpot();
        Reservation active = new Reservation(event, "Student", "student@example.com",
                OffsetDateTime.now().plusMinutes(5));
        when(eventRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(event));
        when(reservationRepository.findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                eq(3L), eq("student@example.com"), any())).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> waitlistService.join(3L,
                new JoinWaitlistRequest("Student", "student@example.com")))
                .isInstanceOf(WaitlistUnavailableException.class)
                .hasMessageContaining("active reservation");
        verify(waitlistEntryRepository, never()).save(any());
    }

    @Test
    void rejectsAnAttendeeAlreadyWaitingForTheEvent() {
        Event event = openEvent(1);
        event.reserveSpot();
        when(eventRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(event));
        when(reservationRepository.findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                eq(3L), eq("student@example.com"), any())).thenReturn(Optional.empty());
        when(waitlistEntryRepository.existsByEvent_IdAndAttendeeEmailIgnoreCaseAndStatus(
                3L, "student@example.com", WaitlistStatus.WAITING)).thenReturn(true);

        assertThatThrownBy(() -> waitlistService.join(3L,
                new JoinWaitlistRequest("Student", "student@example.com")))
                .isInstanceOf(WaitlistUnavailableException.class)
                .hasMessageContaining("already waiting");
        verify(waitlistEntryRepository, never()).save(any());
    }

    private Event openEvent(int capacity) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event("Open Event", null, "UCSD", now.plusDays(5), now.minusDays(1), capacity);
    }
}
