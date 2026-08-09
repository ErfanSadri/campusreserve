package com.erfansadri.campusreserve.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import com.erfansadri.campusreserve.event.Event;

import org.junit.jupiter.api.Test;

class ReservationTests {

    @Test
    void newReservationStartsHeld() {
        Reservation reservation = createReservation();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getHeldUntil()).isNotNull();
    }

    @Test
    void confirmsHeldReservation() {
        Reservation reservation = createReservation();

        reservation.confirm();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getHeldUntil()).isNull();
    }

    @Test
    void cancelsHeldReservation() {
        Reservation reservation = createReservation();

        reservation.cancel();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getHeldUntil()).isNull();
    }

    @Test
    void cancelsConfirmedReservation() {
        Reservation reservation = createReservation();

        reservation.confirm();
        reservation.cancel();

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cannotConfirmCancelledReservation() {
        Reservation reservation = createReservation();

        reservation.cancel();

        assertThatThrownBy(reservation::confirm)
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessage("Only held reservations can be confirmed.");
    }

    private Reservation createReservation() {
        OffsetDateTime now = OffsetDateTime.now();

        Event event = new Event(
                "Test Event",
                null,
                "UCSD",
                now.plusDays(5),
                now.minusDays(1),
                50);

        return new Reservation(
                event,
                "Test Student",
                "student@example.com",
                now.plusMinutes(10));
    }
}