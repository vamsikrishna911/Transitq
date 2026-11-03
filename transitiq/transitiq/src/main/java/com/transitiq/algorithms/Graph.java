package com.transitiq.algorithms;

import java.util.*;

/** Weighted undirected graph stored as an adjacency list: HashMap&lt;Integer, List&lt;Edge&gt;&gt;. */
public class Graph {
    private final Map<Integer, List<Edge>> adj = new HashMap<>();

    public void addVertex(int v) { adj.computeIfAbsent(v, k -> new ArrayList<>()); }

    public void addEdge(Edge e) {
        if (e.distance() <= 0 || e.time() <= 0 || e.fare() < 0) throw new IllegalArgumentException("Edge weights must be positive");
        if (e.from() == e.to()) throw new IllegalArgumentException("Self loops are not allowed");
        addVertex(e.from());
        addVertex(e.to());
        adj.get(e.from()).add(e);
        adj.get(e.to()).add(e.reverse());
    }

    public boolean hasVertex(int v) { return adj.containsKey(v); }
    public List<Edge> neighbors(int v) { requireVertex(v); return adj.get(v); }
    public Set<Integer> vertices() { return adj.keySet(); }
    public int vertexCount() { return adj.size(); }
    public int edgeCount() { int n = 0; for (List<Edge> l : adj.values()) n += l.size(); return n / 2; }

    /** Number of distinct adjacent stops. */
    public int degree(int v) {
        Set<Integer> s = new HashSet<>();
        for (Edge e : neighbors(v)) s.add(e.to());
        return s.size();
    }

    public void requireVertex(int v) {
        if (!adj.containsKey(v)) throw new IllegalArgumentException("Unknown stop id " + v);
    }

    public void requirePair(int s, int t) {
        requireVertex(s);
        requireVertex(t);
        if (s == t) throw new IllegalArgumentException("Source and destination must be different");
    }
}
