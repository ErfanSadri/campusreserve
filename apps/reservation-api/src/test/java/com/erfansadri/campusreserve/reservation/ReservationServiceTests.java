package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.event.EventCache;
import com.erfansadri.campusreserve.event.EventNotFoundException;
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCache eventCache;

    @Mock
    private OutboxEventRecorder outboxEventRecorder;

    @Mock
    private WaitlistPromotionService waitlistPromotionService;

    @Mock
    private CampusReserveMetrics metrics;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createsTenMinuteHoldAndDecrementsCapacity() {
        Event event = openEvent(10);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        when(reservationRepository
                .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                        eq(1L),
                        eq("student@example.com"),
                        any()))
                .thenReturn(Optional.empty());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "  Student@Example.com  ");

        OffsetDateTime before = OffsetDateTime.now();

        ReservationResponse response =
                reservationService.createHold(1L,"test-key",request);

        OffsetDateTime after = OffsetDateTime.now();

        assertThat(response.status())
                .isEqualTo(ReservationStatus.HELD);

        assertThat(response.attendeeEmail())
                .isEqualTo("student@example.com");

        assertThat(event.getRemainingCapacity()).isEqualTo(9);

        assertThat(response.heldUntil())
                .isBetween(
                        before.plusMinutes(10),
                        after.plusMinutes(10));

        ArgumentCaptor<Reservation> captor =
                ArgumentCaptor.forClass(Reservation.class);

        verify(reservationRepository).save(captor.capture());
        verify(eventCache).evict(1L);

        verify(outboxEventRecorder).recordHoldCreated(
                captor.capture(), any(OffsetDateTime.class));
        verify(metrics).holdCreated(any());

        assertThat(captor.getValue().getAttendeeName())
                .isEqualTo("Test Student");

        assertThat(captor.getValue().getAttendeeEmail())
                .isEqualTo("student@example.com");
    }

    @Test
    void rejectsDuplicateActiveReservation() {
        Event event = openEvent(10);

        Reservation existing = new Reservation(
                event,
                "Existing Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(5));

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        when(reservationRepository
                .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                        eq(1L),
                        eq("student@example.com"),
                        any()))
                .thenReturn(Optional.of(existing));

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L,"test-key",request))
                .isInstanceOf(DuplicateReservationException.class)
                .hasMessage(
                        "Attendee already has an active reservation for this event.");

        assertThat(event.getRemainingCapacity()).isEqualTo(10);

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void rejectsReservationWhenEventIsFull() {
        Event event = openEvent(1);
        event.reserveSpot();

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        when(reservationRepository
                .findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                        eq(1L),
                        eq("student@example.com"),
                        any()))
                .thenReturn(Optional.empty());

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L,"test-key",request))
                .isInstanceOf(ReservationUnavailableException.class)
                .hasMessage("Event has no remaining capacity.");

        assertThat(event.getRemainingCapacity()).isZero();

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void rejectsReservationBeforeRegistrationOpens() {
        OffsetDateTime now = OffsetDateTime.now();

        Event event = new Event(
                "Future Registration Event",
                null,
                "UCSD",
                now.plusDays(10),
                now.plusDays(2),
                50);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L,"test-key",request))
                .isInstanceOf(ReservationUnavailableException.class)
                .hasMessage("Registration is not open yet.");

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void rejectsReservationAfterEventStarts() {
        OffsetDateTime now = OffsetDateTime.now();

        Event event = new Event(
                "Past Event",
                null,
                "UCSD",
                now.minusHours(1),
                now.minusDays(5),
                50);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L,"test-key",request))
                .isInstanceOf(ReservationUnavailableException.class)
                .hasMessage("Registration is closed for this event.");

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void throwsWhenEventDoesNotExist() {
        when(eventRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(999L,"test-key",request))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Event 999 was not found.");

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    private Event openEvent(int capacity) {
        OffsetDateTime now = OffsetDateTime.now();

        return new Event(
                "Open Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.minusDays(1),
                capacity);
    }

    @Test
    void returnsReservation() {
        Event event = openEvent(10);

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(10));

        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.getReservation(5L);

        assertThat(response.attendeeEmail())
                .isEqualTo("student@example.com");
        assertThat(response.status())
                .isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void confirmsHeldReservation() {
        Event event = openEvent(10);
        event.reserveSpot();

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(10));

        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.confirmReservation(5L);

        assertThat(response.status())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.heldUntil()).isNull();

        assertThat(event.getRemainingCapacity()).isEqualTo(9);

        verify(outboxEventRecorder).recordConfirmed(
                eq(reservation), any(OffsetDateTime.class));
        verify(metrics).reservationConfirmed();
    }

    @Test
    void cancelsReservationAndReleasesCapacity() {
        Event event = openEvent(10);
        event.reserveSpot();

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(10));

        when(reservationRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(reservation));
        when(eventRepository.findByIdForUpdate(event.getId()))
                .thenReturn(Optional.of(event));

        ReservationResponse response =
                reservationService.cancelReservation(5L);

        assertThat(response.status())
                .isEqualTo(ReservationStatus.CANCELLED);

        assertThat(event.getRemainingCapacity()).isEqualTo(10);

        verify(eventCache).evict(event.getId());
        verify(outboxEventRecorder).recordCancelled(
                eq(reservation), any(OffsetDateTime.class));
        verify(waitlistPromotionService).promoteOldestEligibleWaiter(
                eq(event), any(OffsetDateTime.class));
        verify(metrics).reservationCancelled();
    }

    @Test
    void rejectsConfirmationAfterHoldExpires() {
        Event event = openEvent(10);
        event.reserveSpot();

        Reservation reservation = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().minusMinutes(1));

        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(
                () -> reservationService.confirmReservation(5L))
                .isInstanceOf(ReservationUnavailableException.class)
                .hasMessage("Reservation hold has expired.");

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void throwsWhenReservationDoesNotExist() {
        when(reservationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> reservationService.getReservation(999L))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessage("Reservation 999 was not found.");
    }

    @Test
    void returnsExistingReservationForRepeatedIdempotentRequest() {
        Event event = openEvent(10);
        event.reserveSpot();

        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        String fingerprint =
                ReservationRequestFingerprint.create(
                        1L,
                        "Test Student",
                        "student@example.com");

        Reservation existing = new Reservation(
                event,
                "Test Student",
                "student@example.com",
                OffsetDateTime.now().plusMinutes(10),
                "request-123",
                fingerprint);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        when(reservationRepository
                .findByEvent_IdAndIdempotencyKey(
                        1L,
                        "request-123"))
                .thenReturn(Optional.of(existing));

        ReservationResponse response =
                reservationService.createHold(
                        1L,
                        "request-123",
                        request);

        assertThat(response.status())
                .isEqualTo(ReservationStatus.HELD);

        assertThat(event.getRemainingCapacity())
                .isEqualTo(9);

        verify(reservationRepository, never())
                .save(any(Reservation.class));
        verify(eventCache, never()).evict(any());
        verify(outboxEventRecorder, never())
                .recordHoldCreated(any(), any());
    }

    @Test
    void rejectsMissingIdempotencyKeyBeforeAccessingPersistence() {
        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L, null, request))
                .isInstanceOf(InvalidIdempotencyKeyException.class)
                .hasMessage("Idempotency-Key must contain between 1 and 128 characters.");

        verify(eventRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectsOversizedIdempotencyKeyBeforeAccessingPersistence() {
        CreateReservationRequest request =
                new CreateReservationRequest(
                        "Test Student",
                        "student@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(1L, "x".repeat(129), request))
                .isInstanceOf(InvalidIdempotencyKeyException.class)
                .hasMessage("Idempotency-Key must contain between 1 and 128 characters.");

        verify(eventRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectsIdempotencyKeyReusedWithDifferentRequest() {
        Event event = openEvent(10);
        event.reserveSpot();

        String originalFingerprint =
                ReservationRequestFingerprint.create(
                        1L,
                        "Original Student",
                        "original@example.com");

        Reservation existing = new Reservation(
                event,
                "Original Student",
                "original@example.com",
                OffsetDateTime.now().plusMinutes(10),
                "request-123",
                originalFingerprint);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        when(reservationRepository
                .findByEvent_IdAndIdempotencyKey(
                        1L,
                        "request-123"))
                .thenReturn(Optional.of(existing));

        CreateReservationRequest differentRequest =
                new CreateReservationRequest(
                        "Different Student",
                        "different@example.com");

        assertThatThrownBy(
                () -> reservationService.createHold(
                        1L,
                        "request-123",
                        differentRequest))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage(
                        "Idempotency key was already used with a different request.");

        assertThat(event.getRemainingCapacity())
                .isEqualTo(9);

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }
}
