package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.erfansadri.campusreserve.event.EventRepository;
import com.erfansadri.campusreserve.observability.CampusReserveMetrics;
import com.erfansadri.campusreserve.observability.CampusReserveObservations;
import com.erfansadri.campusreserve.outbox.OutboxEventRecorder;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationProcessorTests {

    @Mock private ReservationRepository reservationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private WaitlistPromotionService waitlistPromotionService;
    @Mock private EventCache eventCache;
    @Mock private OutboxEventRecorder outboxEventRecorder;
    @Mock private CampusReserveMetrics metrics;

    private ReservationExpirationProcessor expirationProcessor;

    @BeforeEach
    void setUp() {
        expirationProcessor = new ReservationExpirationProcessor(
                reservationRepository, eventRepository, waitlistPromotionService,
                eventCache, outboxEventRecorder, 100, metrics,
                new CampusReserveObservations(ObservationRegistry.NOOP));
    }

    @Test
    void expiresAnOverdueHoldReleasesCapacityEvictsCacheAndRecordsOutboxEvent() {
        Event event = openEvent(1);
        setId(event, 7L);
        event.reserveSpot();
        Reservation hold = new Reservation(event, "Student", "student@example.com",
                OffsetDateTime.now().minusMinutes(1));
        setId(hold, 11L);
        OffsetDateTime now = OffsetDateTime.now();

        when(reservationRepository.findOverdueHoldsForUpdateSkipLocked(
                eq(ReservationStatus.HELD), eq(now), any()))
                .thenReturn(List.of(hold));
        when(eventRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(event));

        assertThat(expirationProcessor.expireOverdueHolds(now)).isEqualTo(1);

        assertThat(hold.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(hold.getHeldUntil()).isNull();
        assertThat(event.getRemainingCapacity()).isEqualTo(1);
        verify(outboxEventRecorder).recordExpired(hold, now);
        verify(eventCache).evict(7L);
        verify(metrics).reservationExpired();
    }

    @Test
    void delegatesPromotionAfterExpirationWhileHoldingTheEventLock() {
        Event event = openEvent(1);
        setId(event, 7L);
        event.reserveSpot();
        Reservation hold = new Reservation(event, "Held", "held@example.com",
                OffsetDateTime.now().minusMinutes(1));
        setId(hold, 11L);
        OffsetDateTime now = OffsetDateTime.now();

        when(reservationRepository.findOverdueHoldsForUpdateSkipLocked(
                eq(ReservationStatus.HELD), eq(now), any())).thenReturn(List.of(hold));
        when(eventRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(event));
        expirationProcessor.expireOverdueHolds(now);

        verify(outboxEventRecorder).recordExpired(hold, now);
        verify(waitlistPromotionService).promoteOldestEligibleWaiter(event, now);
    }

    @Test
    void doesNothingWhenNoOverdueHoldsAreClaimed() {
        OffsetDateTime now = OffsetDateTime.now();
        when(reservationRepository.findOverdueHoldsForUpdateSkipLocked(
                eq(ReservationStatus.HELD), eq(now), any())).thenReturn(List.of());

        assertThat(expirationProcessor.expireOverdueHolds(now)).isZero();

        verify(reservationRepository, never()).save(any());
        verify(outboxEventRecorder, never()).recordExpired(any(), any());
        verify(eventCache, never()).evict(any());
    }

    private Event openEvent(int capacity) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event("Open Event", null, "UCSD", now.plusDays(5), now.minusDays(1), capacity);
    }

    private void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
