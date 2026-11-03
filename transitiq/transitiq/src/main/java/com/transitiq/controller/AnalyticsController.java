package com.transitiq.controller;

import com.transitiq.dto.Dtos.*;
import com.transitiq.service.AnalyticsService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) { this.service = service; }

    @GetMapping("/analytics") public AnalyticsResponse analytics() { return service.analytics(); }
    @GetMapping("/analytics/connectivity") public ConnectivityResponse connectivity() { return service.connectivity(); }
    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "UP"); }
}
