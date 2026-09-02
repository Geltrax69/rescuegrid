package com.rescuegrid.engine;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ResourceAllocationEngine {
    private final City city;
    private final EventBus eventBus;
    private final PriorityBlockingQueue<Incident> incidentQueue;
    private final Map<String, Vehicle> assignedVehicles = new ConcurrentHashMap<>();
    private final AllocationConfig config;
    private final ReentrantReadWriteLock allocationLock = new ReentrantReadWriteLock();

    public ResourceAllocationEngine(City city, EventBus eventBus, AllocationConfig config) {
        this.city = city;
        this.eventBus = eventBus;
        this.config = config;
        this.incidentQueue = new PriorityBlockingQueue<Incident>(11, (a, b) -> {
            int severityCompare = b.severity.ordinal() - a.severity.ordinal();
            if (severityCompare != 0) return severityCompare;
            return a.deadline.compareTo(b.deadline);
        });

        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.INCIDENT_CREATED, this::onIncidentCreated);
        eventBus.subscribe(EventType.INCIDENT_RESOLVED, this::onIncidentResolved);
        eventBus.subscribe(EventType.VEHICLE_ARRIVED, this::onVehicleArrived);
        eventBus.subscribe(EventType.VEHICLE_FAILED, this::onVehicleFailed);
    }

    private void onIncidentCreated(Event event) {
        Incident incident = event.get("incident");
        if (incident != null) {
            incidentQueue.add(incident);
            processQueue();
        }
    }

    private void onIncidentResolved(Event event) {
        Incident incident = event.get("incident");
        releaseResources(incident);
    }

    private void onVehicleArrived(Event event) {
        Vehicle vehicle = event.get("vehicle");
        if (vehicle != null) {
            vehicle.arriveAt();
        }
    }

    private void onVehicleFailed(Event event) {
        Vehicle vehicle = event.get("vehicle");
        Incident incident = event.get("incident");
        if (vehicle != null && incident != null) {
            reassignVehicle(vehicle, incident);
        }
    }

    private void processQueue() {
        Incident incident;
        while ((incident = incidentQueue.poll()) != null) {
            if (incident.status != IncidentStatus.OPEN) continue;
            allocateResources(incident);
        }
    }

    private void allocateResources(Incident incident) {
        allocationLock.writeLock().lock();
        try {
            List<ResourceRequirement> requirements = incident.getRequiredResources();
            Map<ResourceType, List<Vehicle>> candidates = new EnumMap<>(ResourceType.class);

            for (ResourceRequirement req : requirements) {
                List<Vehicle> available = findAvailableResources(req.type, req.count);
                if (available.size() >= req.count) {
                    candidates.put(req.type, available);
                } else {
                    // Not enough resources, re-queue with delay
                    incidentQueue.add(incident);
                    return;
                }
            }

            // All resources available, assign them
            for (Map.Entry<ResourceType, List<Vehicle>> entry : candidates.entrySet()) {
                for (int i = 0; i < entry.getValue().size(); i++) {
                    Vehicle v = entry.getValue().get(i);
                    if (v.assignment == Vehicle.AssignmentInfo.NONE) {
                        assignVehicle(v, incident);
                    }
                }
            }
            incident.status = IncidentStatus.ASSIGNED;
            eventBus.publish(new Event(EventType.RESOURCE_ASSIGNED).with("incident", incident));
        } finally {
            allocationLock.writeLock().unlock();
        }
    }

    private List<Vehicle> findAvailableResources(ResourceType type, int count) {
        List<? extends Vehicle> pool;
        switch (type) {
            case AMBULANCE -> pool = city.getAmbulances();
            case FIRE_TRUCK -> pool = city.getFireTrucks();
            case POLICE_VEHICLE -> pool = city.getPoliceVehicles();
            case RESCUE_TEAM -> pool = city.getRescueTeams();
            default -> pool = new java.util.ArrayList<>();
        }

        return pool.stream()
            .filter(v -> v.assignment == Vehicle.AssignmentInfo.NONE)
            .sorted(Comparator.comparingDouble(v -> calculateScore(v, type)))
            .limit(count)
            .map(v -> (Vehicle) v)
            .toList();
    }

    private double calculateScore(Vehicle vehicle, ResourceType type) {
        // priorityScore = severityWeight + distanceWeight + responseTimeWeight + capabilityWeight + workloadWeight
        double severityWeight = config.severityWeight;
        double distanceWeight = config.distanceWeight * vehicle.getEtaTo(vehicle.location); // placeholder
        double responseTimeWeight = config.responseTimeWeight;
        double capabilityWeight = config.capabilityWeight;
        double workloadWeight = config.workloadWeight * (vehicle.currentLoad / Math.max(1.0, vehicle.capacity));

        return severityWeight + distanceWeight + responseTimeWeight + capabilityWeight + workloadWeight;
    }

    private void assignVehicle(Vehicle vehicle, Incident incident) {
        vehicle.assignment = Vehicle.AssignmentInfo.ASSIGNED;
        vehicle.destination = incident.location;
        assignedVehicles.put(vehicle.id, vehicle);
        incident.addAssignedVehicle(new VehiclesAssignment(
            vehicle.resourceType, vehicle.id, 1
        ));
        eventBus.publish(new Event(EventType.VEHICLE_DISPATCHED)
            .with("vehicle", vehicle)
            .with("incident", incident)
        );
    }

    private void releaseResources(Incident incident) {
        for (VehiclesAssignment assignment : incident.getAssignedVehicles()) {
            Vehicle vehicle = assignedVehicles.remove(assignment.vehicleId);
            if (vehicle != null) {
                vehicle.assignment = Vehicle.AssignmentInfo.RETURNING;
                eventBus.publish(new Event(EventType.RESOURCE_RELEASED).with("vehicle", vehicle));
            }
        }
    }

    private void reassignVehicle(Vehicle failedVehicle, Incident incident) {
        failedVehicle.assignment = Vehicle.AssignmentInfo.MAINTENANCE;
        assignedVehicles.remove(failedVehicle.id);
        incident.status = IncidentStatus.OPEN;
        incidentQueue.add(incident);
        eventBus.publish(new Event(EventType.FAILURE_INJECTED)
            .with("vehicle", failedVehicle)
            .with("incident", incident)
        );
    }

    public int getQueuedIncidents() { return incidentQueue.size(); }
    public int getAssignedVehicleCount() { return assignedVehicles.size(); }
}