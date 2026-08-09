package com.erfansadri.campusreserve.reservation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/events/{eventId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createHold(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateReservationRequest request) {

        return reservationService.createHold(eventId, request);
    }

    @GetMapping("/reservations/{reservationId}")
    public ReservationResponse getReservation(
            @PathVariable Long reservationId) {

        return reservationService.getReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public ReservationResponse confirmReservation(
            @PathVariable Long reservationId) {

        return reservationService.confirmReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public ReservationResponse cancelReservation(
            @PathVariable Long reservationId) {

        return reservationService.cancelReservation(reservationId);
    }
}