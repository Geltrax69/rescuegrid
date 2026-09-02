package com.rescuegrid.benchmark;

import com.rescuegrid.algorithm.*;
import com.rescuegrid.model.*;
import java.util.*;

public class BenchmarkManager {
    public static class BenchmarkResult {
        public final String algorithmName;
        public final double avgTimeMs;
        public final double avgDistance;
        public final double avgNodesVisited;

        public BenchmarkResult(String name, double time, double dist, int nodes) {
            this.algorithmName = name;
            this.avgTimeMs = time;
            this.avgDistance = dist;
            this.avgNodesVisited = nodes;
        }

        @Override public String toString() {
            return String.format("%s: avgTime=%.2fms, avgDist=%.1f, nodes=%d", algorithmName, avgTimeMs, avgDistance, (int)avgNodesVisited);
        }
    }

    public List<BenchmarkResult> benchmarkRouters(City city, int iterations) {
        List<BenchmarkResult> results = new ArrayList<>();
        Node[] nodes = city.getAllNodes().toArray(new Node[0]);
        if (nodes.length < 2) return results;

        Router dijkstra = new DijkstraRouter();
        Router astar = new AStarRouter();

        results.add(benchmarkRouter(dijkstra, nodes, iterations, city));
        results.add(benchmarkRouter(astar, nodes, iterations, city));
        return results;
    }

    private BenchmarkResult benchmarkRouter(Router router, Node[] nodes, int iterations, City city) {
        Random random = new Random(42);
        double totalTime = 0;
        double totalDistance = 0;
        int totalNodes = 0;
        int success = 0;

        for (int i = 0; i < iterations; i++) {
            Node from = nodes[random.nextInt(nodes.length)];
            Node to = nodes[random.nextInt(nodes.length)];
            if (from == to) continue;

            long start = System.nanoTime();
            Route route = router.findRoute(city, from, to);
            long end = System.nanoTime();

            double timeMs = (end - start) / 1_000_000.0;
            totalTime += timeMs;
            if (route != null && !route.isEmpty()) {
                totalDistance += route.totalDistance;
                totalNodes += route.getNodeCount();
                success++;
            }
        }

        return new BenchmarkResult(
            router.getName(),
            success > 0 ? totalTime / iterations : 0,
            success > 0 ? totalDistance / success : 0,
            success > 0 ? totalNodes / success : 0
        );
    }

    public String runFullBenchmark(City city) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ALGORITHM BENCHMARK ===\n");
        List<BenchmarkResult> results = benchmarkRouters(city, 100);
        for (BenchmarkResult r : results) {
            sb.append(r).append("\n");
        }
        sb.append("============================\n");
        return sb.toString();
    }
}
