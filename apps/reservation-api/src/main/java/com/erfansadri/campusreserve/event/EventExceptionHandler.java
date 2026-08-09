package com.erfansadri.campusreserve.event;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EventExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEventNotFound(
            EventNotFoundException exception) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidEventException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEvent(
            InvalidEventException exception) {

        return ResponseEntity
                .badRequest()
                .body(Map.of("error", exception.getMessage()));
    }
}