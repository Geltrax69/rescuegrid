package com.rescuegrid.predictive;

import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PredictivePositioning {
    private final City city;
    private final Map<Node, AtomicInteger> incidentHistory = new ConcurrentHashMap<>();
    private final Map<Node, Integer> predictiveStations = new ConcurrentHashMap<>();
    private final List<Incident> historicalIncidents = new CopyOnWriteArrayList<>();

    public PredictivePositioning(City city) {
        this.city = city;
        for (Node node : city.getAllNodes()) {
            incidentHistory.put(node, new AtomicInteger(0));
            predictiveStations.put(node, 0);
        }
    }

    public void recordIncident(Incident incident) {
        if (incident.location != null) {
            incidentHistory.get(incident.location).incrementAndGet();
            historicalIncidents.add(incident);
        }
    }

    public List<Node> getHighRiskZones(int top) {
        return city.getAllNodes().stream()
            .sorted((a, b) -> incidentHistory.get(b).get() - incidentHistory.get(a).get())
            .limit(top)
            .toList();
    }

    public Map<Node, Integer> getPredictiveDistribution() {
        Map<Node, Integer> distribution = new HashMap<>();
        int total = incidentHistory.values().stream().mapToInt(AtomicInteger::get).sum();
        if (total == 0) return distribution;

        int totalVehicles = city.getAllVehicles().size();
        for (Node node : city.getAllNodes()) {
            int incidents = incidentHistory.get(node).get();
            int allocation = totalVehicles * incidents / total;
            distribution.put(node, allocation);
        }
        return distribution;
    }

    public String compareStrategies() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PREDICTIVE vs STATIC POSITIONING ===\n");

        int total = historicalIncidents.size();
        sb.append("Total historical incidents: ").append(total).append("\n");

        if (total > 0) {
            List<Node> highRisk = getHighRiskZones(3);
            sb.append("Top 3 high-risk zones:\n");
            for (Node n : highRisk) {
                sb.append("  ").append(n.id).append(": ")
                  .append(incidentHistory.get(n).get()).append(" incidents\n");
            }

            // Estimate response time for predictive vs static
            double staticAvg = calculateStaticAvgResponseTime();
            double predictiveAvg = calculatePredictiveAvgResponseTime();

            sb.append(String.format("Static avg response time: %.1f min%n", staticAvg));
            sb.append(String.format("Predictive avg response time: %.1f min%n", predictiveAvg));
            sb.append(String.format("Improvement: %.1f%%%n", (staticAvg - predictiveAvg) / staticAvg * 100));
        }

        sb.append("=======================================\n");
        return sb.toString();
    }

    private double calculateStaticAvgResponseTime() {
        if (city.getAllVehicles().isEmpty() || city.getAllNodes().isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (Vehicle v : city.getAllVehicles()) {
            if (v.location == null) continue;
            for (Node node : city.getAllNodes()) {
                sum += v.location.distanceTo(node) / Math.max(1, v.speed) * 60;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private double calculatePredictiveAvgResponseTime() {
        // Predictive: place vehicles near high-risk zones
        Map<Node, Integer> distribution = getPredictiveDistribution();
        if (distribution.isEmpty()) return calculateStaticAvgResponseTime();

        List<Node> topRiskZones = new ArrayList<>(distribution.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList());

        if (topRiskZones.isEmpty()) return calculateStaticAvgResponseTime();

        // Calculate expected distance from nearest vehicle to incident
        double sum = 0;
        int count = 0;
        for (Incident inc : historicalIncidents) {
            if (inc.location == null) continue;
            double minDist = Double.MAX_VALUE;
            for (Vehicle v : city.getAllVehicles()) {
                if (v.location == null) continue;
                double dist = v.location.distanceTo(inc.location);
                if (dist < minDist) minDist = dist;
            }
            if (minDist < Double.MAX_VALUE) {
                sum += minDist / 60.0 * 60; // ~60 km/h
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
}
