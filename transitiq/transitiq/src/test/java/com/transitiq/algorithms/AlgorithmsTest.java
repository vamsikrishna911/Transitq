package com.transitiq.algorithms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Fixture (dist,time,fare): 1-2 (4,10,10)  2-3 (4,10,10)  1-3 (10,5,30)  3-4 (3,6,5)
 * 2-4 (8,30,8)  4-5 (2,3,2)  7-8 (1,1,1); stop 6 isolated.
 * 1->4: via 1-2-3-4 = 11km/26min/25; via 1-3-4 = 13km/11min/35; via 1-2-4 = 12km/40min/18.
 */
class AlgorithmsTest {

    private Graph graph() {
        Graph g = new Graph();
        for (int i = 1; i <= 8; i++) g.addVertex(i);
        g.addEdge(new Edge(1, 2, 4, 10, 10, 1));
        g.addEdge(new Edge(2, 3, 4, 10, 10, 1));
        g.addEdge(new Edge(1, 3, 10, 5, 30, 2));
        g.addEdge(new Edge(3, 4, 3, 6, 5, 2));
        g.addEdge(new Edge(2, 4, 8, 30, 8, 3));
        g.addEdge(new Edge(4, 5, 2, 3, 2, 3));
        g.addEdge(new Edge(7, 8, 1, 1, 1, 4));
        return g;
    }

    @Test void graphConstruction() {
        Graph g = graph();
        assertEquals(8, g.vertexCount());
        assertEquals(7, g.edgeCount());
        assertEquals(3, g.degree(3));
        assertEquals(0, g.degree(6));
        assertTrue(g.neighbors(1).stream().anyMatch(e -> e.to() == 2));
        assertTrue(g.neighbors(2).stream().anyMatch(e -> e.to() == 1), "undirected");
    }

    @Test void bfsMinimumStops() {
        PathResult p = BFS.shortest(graph(), 1, 5).orElseThrow();
        assertEquals(3, p.edges().size());
        assertEquals(1, (int) p.nodes().get(0));
        assertEquals(5, (int) p.nodes().get(p.nodes().size() - 1));
    }

    @Test void dfsComponentsAndReachability() {
        Graph g = graph();
        List<List<Integer>> comps = DFS.components(g);
        assertEquals(3, comps.size());
        assertEquals(List.of(1, 2, 3, 4, 5), comps.get(0));
        assertEquals(List.of(6), comps.get(1));
        assertEquals(List.of(7, 8), comps.get(2));
        assertEquals(5, DFS.reachable(g, 4).size());
    }

    @Test void dijkstraShortestDistance() {
        PathResult p = Dijkstra.shortest(graph(), 1, 4, Mode.DISTANCE).orElseThrow();
        assertEquals(List.of(1, 2, 3, 4), p.nodes());
        assertEquals(11.0, p.distance(), 1e-9);
        assertEquals(26.0, p.time(), 1e-9);
        assertEquals(25.0, p.fare(), 1e-9);
    }

    @Test void dijkstraFastest() {
        PathResult p = Dijkstra.shortest(graph(), 1, 4, Mode.TIME).orElseThrow();
        assertEquals(List.of(1, 3, 4), p.nodes());
        assertEquals(11.0, p.time(), 1e-9);
        assertEquals(13.0, p.distance(), 1e-9);
    }

    @Test void dijkstraCheapest() {
        PathResult p = Dijkstra.shortest(graph(), 1, 4, Mode.FARE).orElseThrow();
        assertEquals(List.of(1, 2, 4), p.nodes());
        assertEquals(18.0, p.fare(), 1e-9);
    }

    @Test void routeReconstructionEdgesMatchNodes() {
        PathResult p = Dijkstra.shortest(graph(), 1, 5, Mode.DISTANCE).orElseThrow();
        assertEquals(List.of(1, 2, 3, 4, 5), p.nodes());
        assertEquals(p.nodes().size() - 1, p.edges().size());
        for (int i = 0; i < p.edges().size(); i++) {
            assertEquals((int) p.nodes().get(i), p.edges().get(i).from());
            assertEquals((int) p.nodes().get(i + 1), p.edges().get(i).to());
        }
        assertEquals(13.0, p.distance(), 1e-9);
    }

    @Test void disconnectedNetwork() {
        Graph g = graph();
        assertEquals(Optional.empty(), BFS.shortest(g, 1, 6));
        assertEquals(Optional.empty(), Dijkstra.shortest(g, 1, 7, Mode.TIME));
        assertTrue(AlternativeRouteFinder.find(g, 1, 8, Mode.DISTANCE, 3).isEmpty());
    }

    @Test void invalidInputs() {
        Graph g = graph();
        assertThrows(IllegalArgumentException.class, () -> BFS.shortest(g, 1, 99));
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.shortest(g, 99, 1, Mode.TIME));
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.shortest(g, 2, 2, Mode.TIME));
        assertThrows(IllegalArgumentException.class, () -> DFS.reachable(g, 42));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(new Edge(1, 2, -1, 1, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> Mode.parse("teleport"));
    }

    @Test void alternativesAreDistinctAndOrdered() {
        List<PathResult> alts = AlternativeRouteFinder.find(graph(), 1, 4, Mode.DISTANCE, 3);
        assertEquals(3, alts.size());
        assertEquals(List.of(1, 2, 3, 4), alts.get(0).nodes());
        assertEquals(List.of(1, 2, 4), alts.get(1).nodes());
        assertEquals(List.of(1, 3, 4), alts.get(2).nodes());
        assertEquals(3, alts.stream().map(PathResult::nodes).distinct().count());
    }
}
