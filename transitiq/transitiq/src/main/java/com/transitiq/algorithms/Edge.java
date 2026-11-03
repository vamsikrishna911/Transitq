package com.transitiq.algorithms;

public record Edge(int from, int to, double distance, double time, double fare, int routeId) {
    public double weight(Mode m) {
        return switch (m) { case DISTANCE -> distance; case TIME -> time; case FARE -> fare; case STOPS -> 1.0; };
    }
    public Edge reverse() { return new Edge(to, from, distance, time, fare, routeId); }
}
