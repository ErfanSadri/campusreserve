package com.erfansadri.campusreserve.waitlist;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WaitlistExceptionHandler {

    @ExceptionHandler(WaitlistUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(
            WaitlistUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }
}
