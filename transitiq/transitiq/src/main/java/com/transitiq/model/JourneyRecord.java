package com.transitiq.model;

public record JourneyRecord(int id, String source, String destination, String mode,
                            double distanceKm, double timeMin, double fare, String lines, String createdAt) {}
