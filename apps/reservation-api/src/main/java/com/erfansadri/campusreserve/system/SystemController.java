package com.erfansadri.campusreserve.system;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SystemController {

    @GetMapping
    public Map<String, String> getSystemStatus() {
        return Map.of(
                "name", "CampusReserve",
                "status", "running");
    }
}