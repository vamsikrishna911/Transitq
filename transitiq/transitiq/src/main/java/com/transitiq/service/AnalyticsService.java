package com.transitiq.service;

import com.transitiq.algorithms.*;
import com.transitiq.dto.Dtos.*;
import com.transitiq.model.Stop;
import com.transitiq.repository.TransitRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final TransitRepository repo;
    private final RouteService routes;

    public AnalyticsService(TransitRepository repo, RouteService routes) { this.repo = repo; this.routes = routes; }

    public AnalyticsResponse analytics() {
        Graph g = routes.buildGraph();
        Map<Integer, String> names = repo.stops().stream().collect(Collectors.toMap(Stop::id, Stop::name));
        List<StopStat> byDegree = new ArrayList<>();
        for (int v : g.vertices()) byDegree.add(new StopStat(v, names.get(v), g.degree(v)));
        byDegree.sort(Comparator.comparingInt(StopStat::connections).reversed().thenComparingInt(StopStat::id));

        List<List<Integer>> comps = DFS.components(g);
        int largest = comps.stream().mapToInt(List::size).max().orElse(0);

        double avgConn = repo.edges().stream().mapToDouble(Edge::distance).average().orElse(0);

        // average shortest-path distance and time over all connected stop pairs (Dijkstra per pair)
        List<Integer> ids = new ArrayList<>(g.vertices());
        Collections.sort(ids);
        double sumD = 0, sumT = 0;
        int pairs = 0;
        for (int i = 0; i < ids.size(); i++)
            for (int j = i + 1; j < ids.size(); j++) {
                Optional<PathResult> d = Dijkstra.shortest(g, ids.get(i), ids.get(j), Mode.DISTANCE);
                Optional<PathResult> t = Dijkstra.shortest(g, ids.get(i), ids.get(j), Mode.TIME);
                if (d.isPresent() && t.isPresent()) { sumD += d.get().distance(); sumT += t.get().time(); pairs++; }
            }

        Map<String, Integer> usage = new HashMap<>();
        for (String l : repo.allJourneyLines())
            if (l != null && !l.isBlank()) for (String c : new HashSet<>(Arrays.asList(l.split(",")))) usage.merge(c, 1, Integer::sum);
        List<RouteStat> popularRoutes = usage.entrySet().stream()
                .map(e -> new RouteStat(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(RouteStat::searches).reversed().thenComparing(RouteStat::line)).limit(5).toList();

        return new AnalyticsResponse(repo.countStops(), repo.countRoutes(), repo.countConnections(),
                byDegree.stream().limit(5).toList(), popularRoutes, repo.recentJourneys(8),
                RouteService.round(avgConn), RouteService.round(pairs == 0 ? 0 : sumD / pairs),
                RouteService.round(pairs == 0 ? 0 : sumT / pairs), comps.size(),
                byDegree.isEmpty() ? null : byDegree.get(0), largest);
    }

    public ConnectivityResponse connectivity() {
        Graph g = routes.buildGraph();
        Map<Integer, String> names = repo.stops().stream().collect(Collectors.toMap(Stop::id, Stop::name));
        List<List<Integer>> comps = DFS.components(g);
        List<ComponentInfo> infos = comps.stream()
                .map(c -> new ComponentInfo(c.size(), c.stream().map(names::get).toList())).toList();
        List<ReachStat> reach = new ArrayList<>();
        for (List<Integer> c : comps) for (int v : c) reach.add(new ReachStat(v, names.get(v), c.size() - 1));
        reach.sort(Comparator.comparingInt(ReachStat::id));
        return new ConnectivityResponse(comps.size(), comps.size() <= 1, infos, reach);
    }
}
