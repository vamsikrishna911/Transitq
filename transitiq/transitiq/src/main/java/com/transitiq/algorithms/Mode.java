package com.transitiq.algorithms;

public enum Mode {
    DISTANCE, TIME, FARE, STOPS;

    public static Mode parse(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("mode is required (DISTANCE, TIME, FARE or STOPS)");
        return switch (s.trim().toUpperCase()) {
            case "DISTANCE", "SHORTEST" -> DISTANCE;
            case "TIME", "FASTEST" -> TIME;
            case "FARE", "CHEAPEST" -> FARE;
            case "STOPS", "MIN_STOPS" -> STOPS;
            default -> throw new IllegalArgumentException("Unknown mode '" + s + "'. Use DISTANCE, TIME, FARE or STOPS");
        };
    }
}
