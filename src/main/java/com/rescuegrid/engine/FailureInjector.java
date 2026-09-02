package com.rescuegrid.engine;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FailureInjector {
    private final City city;
    private final EventBus eventBus;
    private final Random random;
    private volatile boolean running = false;

    public FailureInjector(City city, EventBus eventBus) {
        this.city = city;
        this.eventBus = eventBus;
        this.random = new Random();
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.SIMULATION_STARTED, this::onSimulationStarted);
    }

    public void onSimulationStarted(Event event) {
        // Start injecting failures periodically
        new Thread(this::failureLoop, "FailureInjector").start();
    }

    private void failureLoop() {
        int failureCount = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(2000 + random.nextInt(3000)); // 2-5 second interval
                injectRandomFailure();
                failureCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void injectRandomFailure() {
        int choice = random.nextInt(5);
        switch (choice) {
            case 0 -> injectVehicleBreakdown();
            case 1 -> injectRoadClosure();
            case 2 -> injectHospitalFull();
            case 3 -> injectResourceDepletion();
            case 4 -> injectNewCriticalIncident();
        }
    }

    private void injectVehicleBreakdown() {
        List<Vehicle> vehicles = new ArrayList<>(city.getAllVehicles());
        if (vehicles.isEmpty()) return;

        Vehicle victim = vehicles.get(random.nextInt(vehicles.size()));
        if (victim.assignment == Vehicle.AssignmentInfo.EN_ROUTE || victim.assignment == Vehicle.AssignmentInfo.ON_SCENE) {
            injectVehicleBreakdown(victim);
        }
    }

    private void injectVehicleBreakdown(Vehicle vehicle) {
        vehicle.assignment = Vehicle.AssignmentInfo.MAINTENANCE;
        vehicle.location = null;
        vehicle.destination = null;
        vehicle.roadStatus = RoadStatus.CLOSED;

        eventBus.publish(new Event(EventType.VEHICLE_FAILED)
            .with("vehicle", vehicle)
            .with("failureType", "Breakdown"));

        // Reassign if carrying a patient
        System.out.println("FAILURE: " + vehicle.id + " breakdown - patient reassignment needed");
    }

    private void injectRoadClosure() {
        List<Road> openRoads = new ArrayList<>();
        for (Road road : city.getAllRoads()) {
            if (road.isOpen()) openRoads.add(road);
        }
        if (openRoads.isEmpty()) return;

        Road closedRoad = openRoads.get(random.nextInt(openRoads.size()));
        closedRoad.setStatus(RoadStatus.CLOSED);
        eventBus.publish(new Event(EventType.ROAD_CLOSED).with("road", closedRoad));

        // Recalculate routes for pending incidents
        System.out.println("FAILURE: Road " + closedRoad.id + " closed");
    }

    private void injectHospitalFull() {
        java.util.List<Hospital> hospitals = new java.util.ArrayList<>(city.getAllHospitals());
        if (hospitals.isEmpty()) return;

        Hospital fullHospital = hospitals.get(random.nextInt(hospitals.size()));
        fullHospital.setEmergencyBedCount(0);
        eventBus.publish(new Event(EventType.HOSPITAL_FULL).with("hospital", fullHospital));

        System.out.println("FAILURE: Hospital " + fullHospital.name + " at capacity");
    }

    private void injectResourceDepletion() {
        List<Ambulance> ambulances = city.getAmbulances();
        if (ambulances.isEmpty()) return;

        Ambulance depleted = ambulances.get(random.nextInt(ambulances.size()));
        depleted.currentLoad = depleted.capacity;
        eventBus.publish(new Event(EventType.SUPPLY_DEPLETED)
            .with("vehicle", depleted));

        System.out.println("FAILURE: " + depleted.id + " load depleted to max");
    }

    private void injectNewCriticalIncident() {
        // Find an open node and create a critical incident
        List<Node> intersections = city.getNodesByType(NodeType.INTERSECTION);
        if (intersections.isEmpty()) return;

        Node location = intersections.get(random.nextInt(intersections.size()));
        Incident incident = new Incident(
            "INJECTED-" + System.currentTimeMillis(),
            IncidentType.MEDICAL_EMERGENCY,
            location,
            Severity.CRITICAL,
            5 + random.nextInt(10),
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusMinutes(5)
        );

        eventBus.publish(new Event(EventType.INCIDENT_CREATED).with("incident", incident));
    }
}