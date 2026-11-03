package com.transitiq.controller;

import com.transitiq.dto.Dtos.StopRequest;
import com.transitiq.model.Stop;
import com.transitiq.service.StopService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stops")
public class StopController {
    private final StopService service;

    public StopController(StopService service) { this.service = service; }

    @GetMapping public List<Stop> list() { return service.all(); }
    @GetMapping("/{id}") public Stop get(@PathVariable int id) { return service.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Stop create(@Valid @RequestBody StopRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public Stop update(@PathVariable int id, @Valid @RequestBody StopRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) { service.delete(id); }
}
