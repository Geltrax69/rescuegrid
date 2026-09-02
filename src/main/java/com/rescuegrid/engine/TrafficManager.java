package com.rescuegrid.engine;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficManager {
    private final City city;
    private final EventBus eventBus;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public TrafficManager(City city, EventBus eventBus) {
        this.city = city;
        this.eventBus = eventBus;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TrafficManager");
            t.setDaemon(true);
            return t;
        });
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.INCIDENT_CREATED, this::onIncidentCreated);
        eventBus.subscribe(EventType.VEHICLE_FAILED, this::onVehicleFailed);
        eventBus.subscribe(EventType.INCIDENT_RESOLVED, this::onIncidentResolved);
    }

    private void onIncidentCreated(Event event) {
        Incident incident = event.get("incident");
        if (incident != null) {
            // Increase traffic around incident location
            congestRoadsNear(incident.location, 0.3);
        }
    }

    private void onVehicleFailed(Event event) {
        Vehicle vehicle = event.get("vehicle");
        if (vehicle != null) {
            // Remove vehicles from roads and create congestion
            for (Road road : vehicle.location.getRoads()) {
                road.removeVehicle();
            }
            congestRoadsNear(vehicle.location, 0.2);
        }
    }

    private void onIncidentResolved(Event event) {
        // Traffic gradually decreases
        Incident incident = event.get("incident");
        if (incident != null) {
            congestRoadsNear(incident.location, -0.1);
        }
    }

    private void congestRoadsNear(Node location, double factor) {
        for (Road road : location.getRoads()) {
            if (factor > 0) {
                road.addVehicle();
            } else {
                road.removeVehicle();
            }
        }
    }

    public void startSimulation() {
        running = true;
        // Simulate natural traffic flow every second
        scheduler.scheduleAtFixedRate(this::simulateTrafficFlow, 0, 1, TimeUnit.SECONDS);
        // Random traffic spikes every 10-30 seconds
        scheduler.scheduleAtFixedRate(this::randomTrafficSpike, 10, 20, TimeUnit.SECONDS);
    }

    public void stopSimulation() {
        running = false;
        scheduler.shutdown();
    }

    private void simulateTrafficFlow() {
        for (Road road : city.getAllRoads()) {
            if (!road.isOpen()) continue;
            // Add random vehicles based on road capacity
            if (Math.random() < 0.1 && road.getCurrentVehicles() < road.capacity * 0.9) {
                road.addVehicle();
            } else if (Math.random() < 0.05 && road.getCurrentVehicles() > 0) {
                road.removeVehicle();
            }
        }
        eventBus.publish(new Event(EventType.TRAFFIC_CONGESTED));
    }

    private void randomTrafficSpike() {
        List<Road> roads = new ArrayList<>(city.getAllRoads());
        if (!roads.isEmpty()) {
            Road randomRoad = roads.get((int)(Math.random() * roads.size()));
            if (randomRoad.getCurrentVehicles() < randomRoad.capacity) {
                int extra = (int)(Math.random() * 5) + 1;
                for (int i = 0; i < extra && randomRoad.getCurrentVehicles() < randomRoad.capacity; i++) {
                    randomRoad.addVehicle();
                }
                eventBus.publish(new Event(EventType.TRAFFIC_CONGESTED).with("road", randomRoad));
            }
        }
    }

    public double getAverageTraffic() {
        return city.getAllRoads().stream()
            .mapToDouble(Road::getCongestionFactor)
            .average()
            .orElse(0);
    }

    public int getRoadsWithHighTraffic() {
        return (int) city.getAllRoads().stream()
            .filter(r -> r.getTrafficLevel() == TrafficLevel.HIGH || r.getTrafficLevel() == TrafficLevel.VERY_HIGH)
            .count();
    }
}
