package com.rescuegrid.algorithm;

import com.rescuegrid.model.*;
import java.util.*;
import java.util.stream.*;

public class DijkstraRouter implements Router {
    @Override
    public String getName() { return "Dijkstra"; }

    @Override
    public Route findRoute(City city, Node from, Node to) {
        if (from == null || to == null) return new Route(Collections.emptyList(), 0, 0.0);

        Set<Node> visited = new HashSet<>();
        Map<Node, Node> previous = new HashMap<>();
        Map<Node, Double> distance = new HashMap<>();

        // Initialize distances
        for (Node node : city.getAllNodes()) {
            distance.put(node, Double.MAX_VALUE);
        }
        distance.put(from, 0.0);

        // Priority queue ordered by distance
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(distance::get));
        pq.add(from);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.equals(to)) break;
            if (visited.contains(current)) continue;
            visited.add(current);

            for (Road road : current.getRoads()) {
                Node neighbor = road.from == current ? road.to : road.from;
                if (visited.contains(neighbor)) continue;
                if (!road.isOpen()) continue;

                double newDist = distance.get(current) + road.distance;
                if (newDist < distance.get(neighbor)) {
                    distance.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        // Reconstruct path
        if (!distance.get(to).equals(Double.MAX_VALUE)) {
            List<Node> path = new ArrayList<>();
            Node current = to;
            while (current != null && !current.equals(from)) {
                path.add(current);
                current = previous.get(current);
            }
            path.add(from);
            Collections.reverse(path);

            // Calculate total travel time
            double totalTime = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                Node n1 = path.get(i);
                Node n2 = path.get(i + 1);
                Road road = findRoadBetween(city, n1, n2);
                if (road != null && road.isOpen()) {
                    totalTime += road.getTravelTime();
                }
            }

            return new Route(path, path.size() - 1, totalTime);
        }
        return new Route(Collections.emptyList(), 0, Double.MAX_VALUE);
    }

    private Road findRoadBetween(City city, Node n1, Node n2) {
        return city.getAllRoads().stream()
            .filter(r -> (r.from.equals(n1) && r.to.equals(n2)) || (r.from.equals(n2) && r.to.equals(n1)))
            .findFirst().orElse(null);
    }
}