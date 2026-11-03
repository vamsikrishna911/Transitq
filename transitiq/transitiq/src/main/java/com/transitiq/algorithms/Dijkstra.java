package com.transitiq.algorithms;

import java.util.*;

/** Dijkstra with a PriorityQueue. O((V+E) log V) time, O(V) space. Non-negative weights only. */
public final class Dijkstra {
    private Dijkstra() {}

    private record Entry(int node, double cost) {}

    public static Optional<PathResult> shortest(Graph g, int source, int target, Mode mode) {
        g.requirePair(source, target);
        return shortest(g, source, target, mode, Set.of(), Set.of());
    }

    /** Variant that skips banned nodes and banned directed edges ("u>v"); used by AlternativeRouteFinder. */
    static Optional<PathResult> shortest(Graph g, int source, int target, Mode mode,
                                         Set<Integer> bannedNodes, Set<String> bannedEdges) {
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Edge> prev = new HashMap<>();
        PriorityQueue<Entry> pq = new PriorityQueue<>(Comparator.comparingDouble(Entry::cost));
        dist.put(source, 0.0);
        pq.add(new Entry(source, 0.0));
        while (!pq.isEmpty()) {
            Entry cur = pq.poll();
            if (cur.cost() > dist.getOrDefault(cur.node(), Double.POSITIVE_INFINITY)) continue;
            if (cur.node() == target) break;
            for (Edge e : g.neighbors(cur.node())) {
                if (bannedNodes.contains(e.to()) || bannedEdges.contains(e.from() + ">" + e.to())) continue;
                double nd = cur.cost() + e.weight(mode);
                if (nd < dist.getOrDefault(e.to(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.to(), nd);
                    prev.put(e.to(), e);
                    pq.add(new Entry(e.to(), nd));
                }
            }
        }
        if (!dist.containsKey(target)) return Optional.empty();
        return Optional.of(BFS.rebuild(prev, source, target));
    }
}
