package com.rescuegrid.algorithm;

import com.rescuegrid.model.*;
import java.util.*;

public class AStarRouter implements Router {
    @Override
    public String getName() { return "A*"; }

    @Override
    public Route findRoute(City city, Node from, Node to) {
        if (from == null || to == null) return new Route(Collections.emptyList(), 0, 0.0);

        Set<Node> visited = new HashSet<>();
        Map<Node, Node> previous = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>(); // cost from start
        Map<Node, Double> fScore = new HashMap<>(); // total cost (g + h)

        // Initialize
        for (Node node : city.getAllNodes()) {
            gScore.put(node, Double.MAX_VALUE);
            fScore.put(node, Double.MAX_VALUE);
        }
        gScore.put(from, 0.0);

        // Heuristic: straight-line distance
        double initialH = from.distanceTo(to);
        fScore.put(from, initialH);

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(fScore::get));
        pq.add(from);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (visited.contains(current)) continue;
            if (current.equals(to)) break;
            visited.add(current);

            for (Road road : current.getRoads()) {
                Node neighbor = road.from == current ? road.to : road.from;
                if (visited.contains(neighbor)) continue;
                if (!road.isOpen()) continue;

                double tentativeG = gScore.get(current) + road.distance;
                if (tentativeG < gScore.get(neighbor)) {
                    previous.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    double h = neighbor.distanceTo(to);
                    fScore.put(neighbor, tentativeG + h);
                    pq.add(neighbor);
                }
            }
        }

        // Reconstruct path
        if (gScore.get(to) != Double.MAX_VALUE) {
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