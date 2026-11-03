package com.transitiq.controller;

import com.transitiq.dto.Dtos.*;
import com.transitiq.model.RouteInfo;
import com.transitiq.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) { this.service = service; }

    @GetMapping("/routes") public List<RouteInfo> routes() { return service.routes(); }

    @PostMapping("/routes") @ResponseStatus(HttpStatus.CREATED)
    public RouteInfo create(@Valid @RequestBody RouteRequest r) { return service.createRoute(r); }

    @GetMapping("/route/find")
    public RouteResponse find(@RequestParam int source, @RequestParam int destination,
                              @RequestParam(defaultValue = "TIME") String mode) {
        return service.find(source, destination, mode);
    }

    @GetMapping("/route/alternatives")
    public AlternativesResponse alternatives(@RequestParam int source, @RequestParam int destination,
                                             @RequestParam(defaultValue = "DISTANCE") String mode) {
        return service.alternatives(source, destination, mode);
    }
}
