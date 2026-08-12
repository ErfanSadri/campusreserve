package com.erfansadri.campusreserve.waitlist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

@WebMvcTest(WaitlistController.class)
class WaitlistControllerTests {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private WaitlistService waitlistService;

    @Test
    void createsWaitlistEntry() throws Exception {
        when(waitlistService.join(eq(78L), any(JoinWaitlistRequest.class)))
                .thenReturn(new WaitlistEntryResponse(
                        34L, 78L, "Test Student", "student@example.com",
                        WaitlistStatus.WAITING, OffsetDateTime.now()));

        mockMvc.perform(post("/api/events/78/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attendeeName":"Test Student","attendeeEmail":"student@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(34))
                .andExpect(jsonPath("$.eventId").value(78))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void returnsConflictWhenWaitlistIsUnavailable() throws Exception {
        when(waitlistService.join(eq(78L), any(JoinWaitlistRequest.class)))
                .thenThrow(new WaitlistUnavailableException("Event has available capacity."));

        mockMvc.perform(post("/api/events/78/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"attendeeName":"Test Student","attendeeEmail":"student@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Event has available capacity."));
    }
}
