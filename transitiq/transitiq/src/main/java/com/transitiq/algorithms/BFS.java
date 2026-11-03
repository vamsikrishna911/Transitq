package com.transitiq.algorithms;

import java.util.*;

/** Breadth-first search: route with the fewest edges (stops). O(V+E) time, O(V) space. */
public final class BFS {
    private BFS() {}

    public static Optional<PathResult> shortest(Graph g, int source, int target) {
        g.requirePair(source, target);
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Edge> prev = new HashMap<>();
        queue.add(source);
        visited.add(source);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            if (u == target) break;
            for (Edge e : g.neighbors(u)) {
                if (visited.add(e.to())) { prev.put(e.to(), e); queue.add(e.to()); }
            }
        }
        if (!visited.contains(target)) return Optional.empty();
        return Optional.of(rebuild(prev, source, target));
    }

    static PathResult rebuild(Map<Integer, Edge> prev, int source, int target) {
        LinkedList<Integer> nodes = new LinkedList<>();
        LinkedList<Edge> edges = new LinkedList<>();
        int cur = target;
        nodes.addFirst(cur);
        while (cur != source) { Edge e = prev.get(cur); edges.addFirst(e); cur = e.from(); nodes.addFirst(cur); }
        return PathResult.of(nodes, edges);
    }
}
