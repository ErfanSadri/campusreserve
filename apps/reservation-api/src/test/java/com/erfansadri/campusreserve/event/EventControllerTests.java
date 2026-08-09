package com.erfansadri.campusreserve.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void createsEvent() throws Exception {
        when(eventService.createEvent(any(CreateEventRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Software Engineering Interview Workshop",
                                  "description": "Practice technical interviews.",
                                  "location": "UCSD Price Center",
                                  "startTime": "2026-09-28T18:00:00-07:00",
                                  "registrationOpensAt": "2026-09-20T09:00:00-07:00",
                                  "capacity": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(26))
                .andExpect(jsonPath("$.title")
                        .value("Software Engineering Interview Workshop"))
                .andExpect(jsonPath("$.capacity").value(60))
                .andExpect(jsonPath("$.remainingCapacity").value(60));
    }

    @Test
    void returnsUpcomingEvents() throws Exception {
        when(eventService.getUpcomingEvents())
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(26))
                .andExpect(jsonPath("$[0].title")
                        .value("Software Engineering Interview Workshop"));
    }

    @Test
    void returnsEventById() throws Exception {
        when(eventService.getEvent(26L))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/events/26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(26))
                .andExpect(jsonPath("$.location")
                        .value("UCSD Price Center"));
    }

    @Test
    void returns404WhenEventDoesNotExist() throws Exception {
        when(eventService.getEvent(999999L))
                .thenThrow(new EventNotFoundException(999999L));

        mockMvc.perform(get("/api/events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Event 999999 was not found."));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "location": "",
                                  "startTime": "2026-09-28T18:00:00-07:00",
                                  "registrationOpensAt": "2026-09-20T09:00:00-07:00",
                                  "capacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private EventResponse sampleResponse() {
        return new EventResponse(
                26L,
                "Software Engineering Interview Workshop",
                "Practice technical interviews.",
                "UCSD Price Center",
                OffsetDateTime.parse("2026-09-28T18:00:00-07:00"),
                OffsetDateTime.parse("2026-09-20T09:00:00-07:00"),
                60,
                60,
                OffsetDateTime.parse("2026-08-09T14:07:38-07:00"));
    }
}