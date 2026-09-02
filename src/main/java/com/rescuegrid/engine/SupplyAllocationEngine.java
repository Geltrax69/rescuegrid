package com.rescuegrid.engine;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class SupplyAllocationEngine {
    private final City city;
    private final EventBus eventBus;
    private final AllocationConfig config;
    private final ReentrantLock supplyLock = new ReentrantLock();

    public SupplyAllocationEngine(City city, EventBus eventBus, AllocationConfig config) {
        this.city = city;
        this.eventBus = eventBus;
        this.config = config;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.INCIDENT_CREATED, this::onIncidentCreated);
        eventBus.subscribe(EventType.MASS_CASUALTY, this::onMassCasualty);
    }

    private void onIncidentCreated(Event event) {
        Incident incident = event.get("incident");
        if (incident != null) {
            allocateSupplies(incident);
        }
    }

    private void onMassCasualty(Event event) {
        int count = event.get("patientCount");
        Incident massCasualty = new Incident(
            "MASS-" + System.currentTimeMillis(),
            IncidentType.INDUSTRIAL_ACCIDENT,
            city.getAllNodes().stream().findAny().orElse(null),
            Severity.CRITICAL,
            count,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusMinutes(10)
        );
        allocateSupplies(massCasualty);
    }

    public void allocateSupplies(Incident incident) {
        Map<Supply.SupplyType, Integer> requirements = calculateSupplyRequirements(incident);

        supplyLock.lock();
        try {
            for (Map.Entry<Supply.SupplyType, Integer> entry : requirements.entrySet()) {
                Supply.SupplyType type = entry.getKey();
                int required = entry.getValue();

                Warehouse bestWarehouse = selectWarehouse(type, required, incident.location);
                if (bestWarehouse != null) {
                    bestWarehouse.consumeSupply(type, required);
                    eventBus.publish(new Event(EventType.SUPPLY_DEPLETED)
                        .with("warehouse", bestWarehouse)
                        .with("supplyType", type)
                        .with("quantity", required)
                    );
                } else {
                    System.out.println("Could not fulfill " + type + " x" + required + " for incident " + incident.id);
                }
            }
        } finally {
            supplyLock.unlock();
        }
    }

    private Map<Supply.SupplyType, Integer> calculateSupplyRequirements(Incident incident) {
        Map<Supply.SupplyType, Integer> requirements = new EnumMap<>(Supply.SupplyType.class);
        requirements.put(Supply.SupplyType.MEDICAL_KITS, incident.peopleAffected);
        requirements.put(Supply.SupplyType.BLOOD_UNITS, incident.severity == Severity.CRITICAL ? incident.peopleAffected * 2 : incident.peopleAffected);
        requirements.put(Supply.SupplyType.OXYGEN, incident.peopleAffected / 2 + 1);
        return requirements;
    }

    private Warehouse selectWarehouse(Supply.SupplyType type, int required, Node location) {
        Warehouse best = null;
        double bestScore = Double.MAX_VALUE;

        for (Warehouse warehouse : city.getAllWarehouses()) {
            if (!warehouse.hasSupply(type, required)) continue;

            double distance = warehouse.location.distanceTo(location);
            double distanceScore = distance * config.distanceWeight;

            // Consider road traffic
            double trafficScore = calculateTrafficScore(warehouse.location, location);

            // Consider current load (less loaded = better)
            double loadScore = calculateLoadScore(warehouse) * config.loadWeight;

            // Consider current emergencies (more = worse for fair distribution)
            double emergencyLoad = calculateEmergencyLoad(warehouse.location);

            double totalScore = distanceScore + trafficScore + loadScore + (emergencyLoad * 0.5);
            if (totalScore < bestScore) {
                bestScore = totalScore;
                best = warehouse;
            }
        }

        return best;
    }

    private double calculateTrafficScore(Node from, Node to) {
        double totalTraffic = 0;
        for (Road road : from.getRoads()) {
            if (road.to == to || road.from == to) {
                totalTraffic += road.getCongestionFactor();
            }
        }
        return totalTraffic * config.trafficWeight;
    }

    private double calculateLoadScore(Warehouse warehouse) {
        int totalStock = warehouse.getAvailableSupplies().values().stream()
            .mapToInt(s -> s.quantity)
            .sum();
        return 1.0 / Math.max(1.0, totalStock);
    }

    private double calculateEmergencyLoad(Node location) {
        // Heuristic: more roads = more incidents
        return location.getRoads().size();
    }
}
