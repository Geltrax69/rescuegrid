package com.rescuegrid.simulation;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import com.rescuegrid.engine.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationEngine {
    private final City city;
    private final EventBus eventBus;
    private final IncidentGenerator incidentGenerator;
    private final TrafficManager trafficManager;
    private final FailureInjector failureInjector;
    private final ResourceAllocationEngine resourceAllocationEngine;
    private final HospitalAllocationEngine hospitalAllocationEngine;
    private final SupplyAllocationEngine supplyAllocationEngine;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private final AtomicInteger simulationStep = new AtomicInteger(0);

    public SimulationEngine(City city, EventBus eventBus) {
        this.city = city;
        this.eventBus = eventBus;
        this.incidentGenerator = new IncidentGenerator(city, eventBus);
        this.trafficManager = new TrafficManager(city, eventBus);
        this.failureInjector = new FailureInjector(city, eventBus);
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SimulationEngine");
            t.setDaemon(true);
            return t;
        });

        AllocationConfig config = new AllocationConfig();
        this.resourceAllocationEngine = new ResourceAllocationEngine(city, eventBus, config);
        this.hospitalAllocationEngine = new HospitalAllocationEngine(city, eventBus, config);
        this.supplyAllocationEngine = new SupplyAllocationEngine(city, eventBus, config);

        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.SIMULATION_STARTED, this::onSimulationStarted);
        eventBus.subscribe(EventType.SIMULATION_PAUSED, this::onSimulationPaused);
        eventBus.subscribe(EventType.SIMULATION_RESUMED, this::onSimulationResumed);
        eventBus.subscribe(EventType.SIMULATION_STOPPED, this::onSimulationStopped);
        eventBus.subscribe(EventType.INCIDENT_CREATED, this::onIncidentCreated);
        eventBus.subscribe(EventType.VEHICLE_ARRIVED, this::onVehicleArrived);
    }

    private void onSimulationStarted(Event event) {
        running = true;
        incidentGenerator.startSimulation();
        trafficManager.startSimulation();
        failureInjector.onSimulationStarted(event);
    }

    private void onSimulationPaused(Event event) {
        running = false;
        incidentGenerator.stopSimulation();
        trafficManager.stopSimulation();
    }

    private void onSimulationResumed(Event event) {
        running = true;
        incidentGenerator.startSimulation();
        trafficManager.startSimulation();
    }

    private void onSimulationStopped(Event event) {
        running = false;
        incidentGenerator.stopSimulation();
        trafficManager.stopSimulation();
        scheduler.shutdownNow();
    }

    private void onIncidentCreated(Event event) {
        Incident incident = event.get("incident");
        if (incident != null && incident.severity == Severity.CRITICAL) {
            System.out.printf("CRITICAL INCIDENT: %s at %s (deadline: %s)%n",
                incident.type, incident.location.id, incident.deadline);
        }
    }

    private void onVehicleArrived(Event event) {
        Vehicle vehicle = event.get("vehicle");
        Incident incident = event.get("incident");
        if (vehicle != null && incident != null) {
            // Vehicle arrived at incident location
            vehicle.arrivalTime = LocalDateTime.now();
            incident.setResolved(vehicle.arrivalTime);
            System.out.printf("Vehicle %s arrived at incident %s%n", vehicle.id, incident.id);
        }
    }

    public void start() {
        eventBus.publish(new Event(EventType.SIMULATION_STARTED));
        startSimulationLoop();
    }

    public void stop() {
        eventBus.publish(new Event(EventType.SIMULATION_STOPPED));
    }

    public void pause() {
        eventBus.publish(new Event(EventType.SIMULATION_PAUSED));
    }

    public void resume() {
        eventBus.publish(new Event(EventType.SIMULATION_RESUMED));
    }

    private void startSimulationLoop() {
        // Simulate time steps
        scheduler.scheduleAtFixedRate(this::step, 0, 1, TimeUnit.SECONDS);
        // Log metrics every 10 seconds
        scheduler.scheduleAtFixedRate(this::logMetrics, 10, 10, TimeUnit.SECONDS);
    }

    private void step() {
        simulationStep.incrementAndGet();
        // Update vehicle positions
        updateVehiclePositions();

        eventBus.publish(new Event(EventType.INCIDENT_RESOLVED).with("step", simulationStep.get()));
    }

    private void updateVehiclePositions() {
        for (Vehicle vehicle : city.getAllVehicles()) {
            if (vehicle.assignment == Vehicle.AssignmentInfo.EN_ROUTE && vehicle.destination != null) {
                // Simple movement simulation
                double travelTime = vehicle.getEtaTo(vehicle.destination);
                if (travelTime > 0 && Math.random() < 0.1) { // 10% chance of completing this step
                    vehicle.arriveAt();
                    eventBus.publish(new Event(EventType.VEHICLE_ARRIVED)
                        .with("vehicle", vehicle)
                        .with("incident", incidentByVehicle(vehicle)));
                }
            }
        }
    }

    private Incident incidentByVehicle(Vehicle vehicle) {
        for (Incident incident : incidentGenerator.getRecentIncidents()) {
            if (incident.getAssignedVehicles().stream()
                .anyMatch(a -> a.vehicleId.equals(vehicle.id))) {
                return incident;
            }
        }
        return null;
    }

    private void logMetrics() {
        double avgTraffic = trafficManager.getAverageTraffic();
        int highTrafficRoads = trafficManager.getRoadsWithHighTraffic();
        System.out.printf("Metrics - Step %d | Traffic %d/%d | Incidents %d%n",
            simulationStep.get(),
            highTrafficRoads,
            city.getAllRoads().size(),
            incidentGenerator.getActiveIncidents());
    }

    public int getSimulationStep() { return simulationStep.get(); }
    public boolean isRunning() { return running; }
}

class IncidentGenerator {
    private final City city;
    private final EventBus eventBus;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private final Random random;
    private final List<Incident> recentIncidents = new CopyOnWriteArrayList<>();

    public IncidentGenerator(City city, EventBus eventBus) {
        this.city = city;
        this.eventBus = eventBus;
        this.random = new Random();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IncidentGenerator");
            t.setDaemon(true);
            return t;
        });
    }

    public void startSimulation() {
        running = true;
        scheduler.scheduleAtFixedRate(this::generateIncidents, 0, 5, TimeUnit.SECONDS);
    }

    public void stopSimulation() {
        running = false;
    }

    public List<Incident> getRecentIncidents() { return new ArrayList<>(recentIncidents); }
    public int getActiveIncidents() {
        return (int) recentIncidents.stream().filter(i -> i.status != IncidentStatus.RESOLVED).count();
    }

    private void generateIncidents() {
        if (!running) return;

        // Generate 0-3 incidents per cycle
        for (int i = 0; i < random.nextInt(4); i++) {
            generateSingleIncident();
        }
    }

    private void generateSingleIncident() {
        List<Node> availableNodes = city.getNodesByType(NodeType.INTERSECTION);
        if (availableNodes.isEmpty()) return;

        Node location = availableNodes.get(random.nextInt(availableNodes.size()));
        IncidentType type = randomIncidentType();
        Severity severity = randomSeverity(type);
        int peopleAffected = randomPeopleCount(type);

        Incident incident = new Incident(
            "INC-" + System.currentTimeMillis() + "-" + (random.nextInt(1000)),
            type,
            location,
            severity,
            peopleAffected,
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(random.nextInt(10) + 2)
        );

        eventBus.publish(new Event(EventType.INCIDENT_CREATED).with("incident", incident));
        recentIncidents.add(incident);
        cleanupOldIncidents();

        System.out.printf("Incident created: %s (Severity: %s, Deadline: %s)%n",
            incident, severity, incident.deadline);
    }

    private IncidentType randomIncidentType() {
        IncidentType[] types = IncidentType.values();
        return types[random.nextInt(types.length)];
    }

    private Severity randomSeverity(IncidentType type) {
        if (type == IncidentType.MEDICAL_EMERGENCY) {
            return random.nextDouble() < 0.2 ? Severity.CRITICAL : Severity.HIGH;
        } else if (type == IncidentType.BUILDING_COLLAPSE || type == IncidentType.INDUSTRIAL_ACCIDENT) {
            return random.nextDouble() < 0.3 ? Severity.CRITICAL : Severity.HIGH;
        } else {
            return Severity.MEDIUM;
        }
    }

    private int randomPeopleCount(IncidentType type) {
        return random.nextInt(10) + 1;
    }

    private void cleanupOldIncidents() {
        recentIncidents.removeIf(incident -> incident.status == IncidentStatus.RESOLVED);
        if (recentIncidents.size() > 100) {
            recentIncidents.removeIf(i -> i.creationTime.isBefore(LocalDateTime.now().minusHours(1)));
        }
    }
}