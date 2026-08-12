package com.erfansadri.campusreserve.waitlist;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/{eventId}/waitlist")
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistEntryResponse join(
            @PathVariable Long eventId,
            @Valid @RequestBody JoinWaitlistRequest request) {
        return waitlistService.join(eventId, request);
    }
}
