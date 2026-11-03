package com.transitiq.repository;

import com.transitiq.algorithms.Edge;
import com.transitiq.dto.Dtos.StopRequest;
import com.transitiq.model.JourneyRecord;
import com.transitiq.model.RouteInfo;
import com.transitiq.model.Stop;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TransitRepository {
    private final JdbcTemplate jdbc;

    public TransitRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Stop> STOP = (rs, i) -> new Stop(rs.getInt("id"), rs.getString("name"),
            rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("description"));

    // ---- stops ----
    public List<Stop> stops() { return jdbc.query("SELECT * FROM bus_stops ORDER BY id", STOP); }

    public Optional<Stop> stop(int id) {
        return jdbc.query("SELECT * FROM bus_stops WHERE id = ?", STOP, id).stream().findFirst();
    }

    public int insertStop(StopRequest r) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bus_stops (name, latitude, longitude, description) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.name().trim());
            ps.setDouble(2, r.latitude());
            ps.setDouble(3, r.longitude());
            ps.setString(4, r.description());
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).intValue();
    }

    public boolean updateStop(int id, StopRequest r) {
        return jdbc.update("UPDATE bus_stops SET name=?, latitude=?, longitude=?, description=? WHERE id=?",
                r.name().trim(), r.latitude(), r.longitude(), r.description(), id) > 0;
    }

    public boolean deleteStop(int id) { return jdbc.update("DELETE FROM bus_stops WHERE id = ?", id) > 0; }

    // ---- connections / routes ----
    public List<Edge> edges() {
        return jdbc.query("SELECT * FROM connections ORDER BY id", (rs, i) -> new Edge(rs.getInt("from_stop_id"),
                rs.getInt("to_stop_id"), rs.getDouble("distance_km"), rs.getDouble("time_min"),
                rs.getDouble("fare"), rs.getInt("route_id")));
    }

    public Map<Integer, String> routeCodes() {
        Map<Integer, String> m = new HashMap<>();
        jdbc.query("SELECT r.id, b.code FROM routes r JOIN buses b ON b.id = r.bus_id",
                rs -> { m.put(rs.getInt("id"), rs.getString("code")); });
        return m;
    }

    public List<RouteInfo> routes() {
        Map<Integer, List<Integer>> seq = new HashMap<>();
        jdbc.query("SELECT route_id, stop_id FROM route_stops ORDER BY route_id, seq",
                rs -> { seq.computeIfAbsent(rs.getInt("route_id"), k -> new ArrayList<>()).add(rs.getInt("stop_id")); });
        return jdbc.query("SELECT r.id, b.code, r.name FROM routes r JOIN buses b ON b.id = r.bus_id ORDER BY r.id",
                (rs, i) -> new RouteInfo(rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                        seq.getOrDefault(rs.getInt("id"), List.of())));
    }

    public int findOrCreateBus(String code, String name) {
        List<Integer> ids = jdbc.queryForList("SELECT id FROM buses WHERE code = ?", Integer.class, code);
        if (!ids.isEmpty()) return ids.get(0);
        return insertKey("INSERT INTO buses (code, name) VALUES (?, ?)", code, name);
    }

    public int insertRoute(int busId, String name) {
        return insertKey("INSERT INTO routes (bus_id, name) VALUES (?, ?)", busId, name);
    }

    public void insertRouteStop(int routeId, int stopId, int seq) {
        jdbc.update("INSERT INTO route_stops (route_id, stop_id, seq) VALUES (?,?,?)", routeId, stopId, seq);
    }

    public void insertConnection(int from, int to, int routeId, double dist, double time, double fare) {
        jdbc.update("INSERT INTO connections (from_stop_id,to_stop_id,route_id,distance_km,time_min,fare) VALUES (?,?,?,?,?,?)",
                from, to, routeId, dist, time, fare);
    }

    private int insertKey(String sql, Object... args) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).intValue();
    }

    // ---- journeys ----
    public void saveJourney(int src, int dst, String mode, double d, double t, double f, String lines) {
        jdbc.update("INSERT INTO journeys (source_id,destination_id,mode,distance_km,time_min,fare,line_codes) VALUES (?,?,?,?,?,?,?)",
                src, dst, mode, d, t, f, lines);
    }

    public List<JourneyRecord> recentJourneys(int limit) {
        return jdbc.query("SELECT j.id, s.name AS src, d.name AS dst, j.mode, j.distance_km, j.time_min, j.fare, j.line_codes, j.created_at "
                        + "FROM journeys j JOIN bus_stops s ON s.id = j.source_id JOIN bus_stops d ON d.id = j.destination_id "
                        + "ORDER BY j.id DESC LIMIT ?",
                (rs, i) -> new JourneyRecord(rs.getInt("id"), rs.getString("src"), rs.getString("dst"), rs.getString("mode"),
                        rs.getDouble("distance_km"), rs.getDouble("time_min"), rs.getDouble("fare"),
                        rs.getString("line_codes"), String.valueOf(rs.getTimestamp("created_at"))), limit);
    }

    public List<String> allJourneyLines() { return jdbc.queryForList("SELECT line_codes FROM journeys", String.class); }

    public int countStops() { return count("bus_stops"); }
    public int countRoutes() { return count("routes"); }
    public int countConnections() { return count("connections"); }
    private int count(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }
}
