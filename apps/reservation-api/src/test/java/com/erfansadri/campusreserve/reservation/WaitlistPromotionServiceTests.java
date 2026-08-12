package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.waitlist.WaitlistEntry;
import com.erfansadri.campusreserve.waitlist.WaitlistEntryRepository;
import com.erfansadri.campusreserve.waitlist.WaitlistStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WaitlistPromotionServiceTests {

    @Mock private WaitlistEntryRepository waitlistEntryRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private OutboxEventRecorder outboxEventRecorder;
    @Mock private CampusReserveMetrics metrics;
    @InjectMocks private WaitlistPromotionService promotionService;

    @Test
    void skipsIneligibleWaiterAndPromotesTheOldestEligibleWaiter() {
        Event event = openFullEvent();
        setId(event, 7L);
        WaitlistEntry ineligible = new WaitlistEntry(event, "First", "first@example.com");
        WaitlistEntry eligible = new WaitlistEntry(event, "Second", "second@example.com");
        OffsetDateTime now = OffsetDateTime.now();

        when(waitlistEntryRepository.findByEvent_IdAndStatusOrderByCreatedAtAscIdAsc(
                7L, WaitlistStatus.WAITING)).thenReturn(List.of(ineligible, eligible));
        when(reservationRepository.findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                eq(7L), eq("first@example.com"), any())).thenReturn(Optional.of(
                        new Reservation(event, "First", "first@example.com", now.plusMinutes(1))));
        when(reservationRepository.findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
                eq(7L), eq("second@example.com"), any())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        promotionService.promoteOldestEligibleWaiter(event, now);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getAttendeeEmail()).isEqualTo("second@example.com");
        assertThat(captor.getValue().getHeldUntil()).isEqualTo(now.plusMinutes(10));
        assertThat(ineligible.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(eligible.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(event.getRemainingCapacity()).isZero();
        verify(outboxEventRecorder).recordHoldCreated(captor.getValue(), now);
        verify(metrics).waitlistPromoted();
    }

    private Event openFullEvent() {
        OffsetDateTime now = OffsetDateTime.now();
        Event event = new Event("Event", null, "UCSD", now.plusDays(1), now.minusDays(1), 1);
        event.reserveSpot();
        event.releaseSpot();
        return event;
    }

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
