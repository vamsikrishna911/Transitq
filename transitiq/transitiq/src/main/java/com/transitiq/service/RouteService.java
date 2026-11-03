package com.transitiq.service;

import com.transitiq.algorithms.*;
import com.transitiq.dto.Dtos.*;
import com.transitiq.model.RouteInfo;
import com.transitiq.model.Stop;
import com.transitiq.repository.TransitRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {
    private final TransitRepository repo;

    public RouteService(TransitRepository repo) { this.repo = repo; }

    /** Builds the in-memory graph from the database (every stop is a vertex, every connection an undirected edge). */
    public Graph buildGraph() {
        Graph g = new Graph();
        for (Stop s : repo.stops()) g.addVertex(s.id());
        for (Edge e : repo.edges()) g.addEdge(e);
        return g;
    }

    public List<RouteInfo> routes() { return repo.routes(); }

    public RouteResponse find(int source, int destination, String modeText) {
        Mode mode = Mode.parse(modeText);
        Graph g = buildGraph();
        g.requirePair(source, destination);
        Map<Integer, Stop> stops = stopMap();
        Optional<PathResult> path = mode == Mode.STOPS ? BFS.shortest(g, source, destination)
                                                       : Dijkstra.shortest(g, source, destination, mode);
        if (path.isEmpty()) return notFound(stops, source, destination, mode);
        RouteResponse r = toResponse(path.get(), mode, stops);
        repo.saveJourney(source, destination, mode.name(), r.distanceKm(), r.timeMin(), r.fare(), String.join(",", r.lines()));
        return r;
    }

    public AlternativesResponse alternatives(int source, int destination, String modeText) {
        Mode mode = Mode.parse(modeText);
        Graph g = buildGraph();
        g.requirePair(source, destination);
        Map<Integer, Stop> stops = stopMap();
        List<PathResult> paths = AlternativeRouteFinder.find(g, source, destination, mode, 3);
        if (paths.isEmpty()) {
            return new AlternativesResponse(false, notFound(stops, source, destination, mode).message(), List.of());
        }
        return new AlternativesResponse(true, null, paths.stream().map(p -> toResponse(p, mode, stops)).toList());
    }

    @Transactional
    public RouteInfo createRoute(RouteRequest req) {
        Map<Integer, Stop> stops = stopMap();
        List<Integer> ids = req.stopIds();
        for (int i = 0; i < ids.size(); i++) {
            if (!stops.containsKey(ids.get(i))) throw new IllegalArgumentException("Unknown stop id " + ids.get(i));
            if (i > 0 && ids.get(i).equals(ids.get(i - 1))) throw new IllegalArgumentException("Consecutive stops must differ");
        }
        int busId = repo.findOrCreateBus(req.busCode().trim(), req.name().trim());
        int routeId = repo.insertRoute(busId, req.name().trim());
        for (int i = 0; i < ids.size(); i++) repo.insertRouteStop(routeId, ids.get(i), i + 1);
        for (int i = 1; i < ids.size(); i++) {
            Stop a = stops.get(ids.get(i - 1)), b = stops.get(ids.get(i));
            double d = Math.max(0.1, round(haversineKm(a, b) * 1.25));   // road factor
            repo.insertConnection(a.id(), b.id(), routeId, d, round(d / 25.0 * 60 + 1.5), round(1.5 + 2.0 * d));
        }
        return repo.routes().stream().filter(r -> r.id() == routeId).findFirst().orElseThrow();
    }

    private Map<Integer, Stop> stopMap() {
        return repo.stops().stream().collect(Collectors.toMap(Stop::id, s -> s, (a, b) -> a, LinkedHashMap::new));
    }

    private RouteResponse notFound(Map<Integer, Stop> stops, int s, int t, Mode mode) {
        String msg = "No route exists between " + stops.get(s).name() + " and " + stops.get(t).name()
                + ": these stops are in disconnected parts of the network.";
        return new RouteResponse(false, msg, mode.name(), List.of(), List.of(), 0, 0, 0, 0);
    }

    private RouteResponse toResponse(PathResult p, Mode mode, Map<Integer, Stop> stops) {
        Map<Integer, String> codes = repo.routeCodes();
        List<String> lines = new ArrayList<>();
        for (Edge e : p.edges()) {
            String c = codes.getOrDefault(e.routeId(), "?");
            if (lines.isEmpty() || !lines.get(lines.size() - 1).equals(c)) lines.add(c);
        }
        List<Stop> path = p.nodes().stream().map(stops::get).toList();
        return new RouteResponse(true, null, mode.name(), path, lines, round(p.distance()), round(p.time()),
                round(p.fare()), path.size());
    }

    static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    static double haversineKm(Stop a, Stop b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude()), dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371.0 * 2 * Math.asin(Math.sqrt(h));
    }
}
