package com.transitiq.dto;

import com.transitiq.model.JourneyRecord;
import com.transitiq.model.Stop;
import jakarta.validation.constraints.*;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record StopRequest(
            @NotBlank(message = "name is required") @Size(max = 100, message = "name must be at most 100 characters") String name,
            @NotNull(message = "latitude is required") @DecimalMin(value = "-90", message = "latitude must be >= -90") @DecimalMax(value = "90", message = "latitude must be <= 90") Double latitude,
            @NotNull(message = "longitude is required") @DecimalMin(value = "-180", message = "longitude must be >= -180") @DecimalMax(value = "180", message = "longitude must be <= 180") Double longitude,
            @Size(max = 255, message = "description must be at most 255 characters") String description) {}

    public record RouteRequest(
            @NotBlank(message = "busCode is required") @Size(max = 20) String busCode,
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            @NotNull(message = "stopIds is required") @Size(min = 2, message = "a route needs at least 2 stops") List<Integer> stopIds) {}

    public record RouteResponse(boolean found, String message, String mode, List<Stop> stops, List<String> lines,
                                double distanceKm, double timeMin, double fare, int stopCount) {}

    public record AlternativesResponse(boolean found, String message, List<RouteResponse> routes) {}

    public record StopStat(int id, String name, int connections) {}
    public record RouteStat(String line, int searches) {}
    public record ReachStat(int id, String name, int reachable) {}
    public record ComponentInfo(int size, List<String> stops) {}

    public record AnalyticsResponse(int totalStops, int totalRoutes, int totalConnections,
                                    List<StopStat> popularStops, List<RouteStat> popularRoutes,
                                    List<JourneyRecord> recentSearches, double avgConnectionDistanceKm,
                                    double avgJourneyDistanceKm, double avgJourneyTimeMin,
                                    int connectedComponents, StopStat mostConnected, int largestComponentSize) {}

    public record ConnectivityResponse(int componentCount, boolean fullyConnected,
                                       List<ComponentInfo> components, List<ReachStat> reachableByStop) {}
}
