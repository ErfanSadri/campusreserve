package com.erfansadri.campusreserve.reservation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void createsReservationHold() throws Exception {
        when(reservationService.createHold(
                eq(78L),
                any(CreateReservationRequest.class)))
                .thenReturn(heldResponse());

        mockMvc.perform(post("/api/events/78/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attendeeName": "Test Student",
                                  "attendeeEmail": "student@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(34))
                .andExpect(jsonPath("$.eventId").value(78))
                .andExpect(jsonPath("$.attendeeName")
                        .value("Test Student"))
                .andExpect(jsonPath("$.attendeeEmail")
                        .value("student@example.com"))
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    void returnsReservation() throws Exception {
        when(reservationService.getReservation(34L))
                .thenReturn(heldResponse());

        mockMvc.perform(get("/api/reservations/34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(34))
                .andExpect(jsonPath("$.eventId").value(78))
                .andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    void confirmsReservation() throws Exception {
        when(reservationService.confirmReservation(34L))
                .thenReturn(confirmedResponse());

        mockMvc.perform(post("/api/reservations/34/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.heldUntil").isEmpty());
    }

    @Test
    void cancelsReservation() throws Exception {
        when(reservationService.cancelReservation(34L))
                .thenReturn(cancelledResponse());

        mockMvc.perform(post("/api/reservations/34/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.heldUntil").isEmpty());
    }

    @Test
    void returns404WhenReservationDoesNotExist() throws Exception {
        when(reservationService.getReservation(999L))
                .thenThrow(new ReservationNotFoundException(999L));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Reservation 999 was not found."));
    }

    @Test
    void returns409ForDuplicateReservation() throws Exception {
        when(reservationService.createHold(
                eq(78L),
                any(CreateReservationRequest.class)))
                .thenThrow(new DuplicateReservationException());

        mockMvc.perform(post("/api/events/78/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attendeeName": "Test Student",
                                  "attendeeEmail": "student@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Attendee already has an active reservation for this event."));
    }

    @Test
    void returns409WhenReservationUnavailable() throws Exception {
        when(reservationService.createHold(
                eq(78L),
                any(CreateReservationRequest.class)))
                .thenThrow(new ReservationUnavailableException(
                        "Event has no remaining capacity."));

        mockMvc.perform(post("/api/events/78/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attendeeName": "Test Student",
                                  "attendeeEmail": "student@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Event has no remaining capacity."));
    }

    @Test
    void returns409ForInvalidReservationState() throws Exception {
        when(reservationService.confirmReservation(34L))
                .thenThrow(new InvalidReservationStateException(
                        "Only held reservations can be confirmed."));

        mockMvc.perform(post("/api/reservations/34/confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Only held reservations can be confirmed."));
    }

    @Test
    void rejectsInvalidReservationRequest() throws Exception {
        mockMvc.perform(post("/api/events/78/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attendeeName": "",
                                  "attendeeEmail": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private ReservationResponse heldResponse() {
        return new ReservationResponse(
                34L,
                78L,
                "Test Student",
                "student@example.com",
                ReservationStatus.HELD,
                OffsetDateTime.parse("2026-08-09T15:13:41-07:00"),
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"),
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"));
    }

    private ReservationResponse confirmedResponse() {
        return new ReservationResponse(
                34L,
                78L,
                "Test Student",
                "student@example.com",
                ReservationStatus.CONFIRMED,
                null,
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"),
                OffsetDateTime.parse("2026-08-09T15:05:00-07:00"));
    }

    private ReservationResponse cancelledResponse() {
        return new ReservationResponse(
                34L,
                78L,
                "Test Student",
                "student@example.com",
                ReservationStatus.CANCELLED,
                null,
                OffsetDateTime.parse("2026-08-09T15:03:41-07:00"),
                OffsetDateTime.parse("2026-08-09T15:05:24-07:00"));
    }
}