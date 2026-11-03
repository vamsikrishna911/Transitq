package com.transitiq.algorithms;

import java.util.List;

public record PathResult(List<Integer> nodes, List<Edge> edges, double distance, double time, double fare) {
    public static PathResult of(List<Integer> nodes, List<Edge> edges) {
        double d = 0, t = 0, f = 0;
        for (Edge e : edges) { d += e.distance(); t += e.time(); f += e.fare(); }
        return new PathResult(List.copyOf(nodes), List.copyOf(edges), d, t, f);
    }
    public double cost(Mode m) {
        return switch (m) { case DISTANCE -> distance; case TIME -> time; case FARE -> fare; case STOPS -> edges.size(); };
    }
}
