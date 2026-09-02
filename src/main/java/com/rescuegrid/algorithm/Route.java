package com.rescuegrid.algorithm;

import com.rescuegrid.model.Node;
import java.util.List;
import java.util.Collections;

public class Route {
    public final List<Node> path;
    public final int edgesCount;
    public final double totalDistance;
    public final double estimatedTimeMinutes;

    public Route(List<Node> path, int edgesCount, double estimatedTimeMinutes) {
        this.path = path != null ? List.copyOf(path) : Collections.emptyList();
        this.edgesCount = edgesCount;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.totalDistance = calculateDistance();
    }

    private double calculateDistance() {
        double dist = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            dist += path.get(i).distanceTo(path.get(i + 1));
        }
        return dist;
    }

    public Node getStart() { return path.isEmpty() ? null : path.get(0); }
    public Node getEnd() { return path.isEmpty() ? null : path.get(path.size() - 1); }
    public boolean isEmpty() { return path.isEmpty(); }
    public int getNodeCount() { return path.size(); }

    @Override public String toString() {
        return "Route{" + edgesCount + " edges, " + String.format("%.1f", totalDistance) + "km, " + String.format("%.0f", estimatedTimeMinutes) + "min}";
    }
}