package com.transitiq.algorithms;

import java.util.*;

/**
 * Yen's k-shortest loopless paths, bounded to k candidates and to routes whose cost is at most
 * BOUND x the best route. Returns distinct node sequences, best first.
 */
public final class AlternativeRouteFinder {
    private static final double BOUND = 2.5;
    private AlternativeRouteFinder() {}

    public static List<PathResult> find(Graph g, int source, int target, Mode mode, int k) {
        g.requirePair(source, target);
        List<PathResult> accepted = new ArrayList<>();
        Optional<PathResult> first = Dijkstra.shortest(g, source, target, mode);
        if (first.isEmpty()) return accepted;
        accepted.add(first.get());
        double limit = first.get().cost(mode) * BOUND;
        List<PathResult> candidates = new ArrayList<>();

        for (int n = 1; n < k; n++) {
            PathResult last = accepted.get(n - 1);
            for (int i = 0; i < last.nodes().size() - 1; i++) {
                int spur = last.nodes().get(i);
                List<Integer> root = last.nodes().subList(0, i + 1);
                Set<String> bannedEdges = new HashSet<>();
                for (PathResult p : accepted)
                    if (p.nodes().size() > i + 1 && p.nodes().subList(0, i + 1).equals(root))
                        bannedEdges.add(p.nodes().get(i) + ">" + p.nodes().get(i + 1));
                Set<Integer> bannedNodes = new HashSet<>(root.subList(0, i));
                Optional<PathResult> spurPath = Dijkstra.shortest(g, spur, target, mode, bannedNodes, bannedEdges);
                if (spurPath.isEmpty()) continue;
                List<Integer> nodes = new ArrayList<>(root.subList(0, i));
                nodes.addAll(spurPath.get().nodes());
                List<Edge> edges = new ArrayList<>(last.edges().subList(0, i));
                edges.addAll(spurPath.get().edges());
                PathResult cand = PathResult.of(nodes, edges);
                if (cand.cost(mode) <= limit && !has(accepted, cand) && !has(candidates, cand)) candidates.add(cand);
            }
            if (candidates.isEmpty()) break;
            candidates.sort(Comparator.comparingDouble(p -> p.cost(mode)));
            accepted.add(candidates.remove(0));
        }
        return accepted;
    }

    private static boolean has(List<PathResult> list, PathResult p) {
        for (PathResult x : list) if (x.nodes().equals(p.nodes())) return true;
        return false;
    }
}
