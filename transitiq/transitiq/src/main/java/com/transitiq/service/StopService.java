package com.transitiq.service;

import com.transitiq.dto.Dtos.StopRequest;
import com.transitiq.model.Stop;
import com.transitiq.repository.TransitRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class StopService {
    private final TransitRepository repo;

    public StopService(TransitRepository repo) { this.repo = repo; }

    public List<Stop> all() { return repo.stops(); }

    public Stop get(int id) { return repo.stop(id).orElseThrow(() -> new NoSuchElementException("Stop " + id + " not found")); }

    public Stop create(StopRequest r) { return get(repo.insertStop(r)); }

    public Stop update(int id, StopRequest r) {
        if (!repo.updateStop(id, r)) get(id); // throws 404 when the id does not exist
        return get(id);
    }

    public void delete(int id) {
        if (!repo.deleteStop(id)) throw new NoSuchElementException("Stop " + id + " not found");
    }
}
