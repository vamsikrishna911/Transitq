package com.transitiq.algorithms;

import java.util.*;

/** Depth-first search (iterative, explicit stack) for connectivity. O(V+E) time, O(V) space. */
public final class DFS {
    private DFS() {}

    public static Set<Integer> reachable(Graph g, int start) {
        g.requireVertex(start);
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (!visited.add(u)) continue;
            for (Edge e : g.neighbors(u)) if (!visited.contains(e.to())) stack.push(e.to());
        }
        return visited;
    }

    /** Connected components, each sorted; components ordered by smallest stop id. */
    public static List<List<Integer>> components(Graph g) {
        List<Integer> ids = new ArrayList<>(g.vertices());
        Collections.sort(ids);
        Set<Integer> seen = new HashSet<>();
        List<List<Integer>> result = new ArrayList<>();
        for (int v : ids) {
            if (seen.contains(v)) continue;
            Set<Integer> comp = reachable(g, v);
            seen.addAll(comp);
            List<Integer> sorted = new ArrayList<>(comp);
            Collections.sort(sorted);
            result.add(sorted);
        }
        return result;
    }
}
