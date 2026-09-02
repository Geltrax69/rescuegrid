package com.rescuegrid.model;

import java.time.LocalDateTime;
import java.util.*;

public class Incident implements Comparable<Incident> {
    public final String id;
    public final IncidentType type;
    public final Node location;
    public final Severity severity;
    public final int peopleAffected;
    public final LocalDateTime creationTime;
    public final LocalDateTime deadline;
    public volatile IncidentStatus status = IncidentStatus.OPEN;
    private final List<ResourceRequirement> requiredResources = new ArrayList<>();
    private final Set<VehiclesAssignment> assignedVehicles = new HashSet<>();
    private LocalDateTime resolutionTime;

    public Incident(String id, IncidentType type, Node location, Severity severity,
                    int peopleAffected, LocalDateTime creationTime, LocalDateTime deadline) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.severity = severity;
        this.peopleAffected = peopleAffected;
        this.creationTime = creationTime;
        this.deadline = deadline;
        initializeRequirements();
    }

    private void initializeRequirements() {
        switch (type) {
            case MEDICAL_EMERGENCY -> {
                requiredResources.add(new ResourceRequirement(ResourceType.AMBULANCE, 1));
                requiredResources.add(new ResourceRequirement(ResourceType.MEDICAL_KITS, 2));
            }
            case FIRE -> {
                requiredResources.add(new ResourceRequirement(ResourceType.FIRE_TRUCK, 1));
                requiredResources.add(new ResourceRequirement(ResourceType.RESCUE_TEAM, 2));
            }
            case ACCIDENT -> {
                requiredResources.add(new ResourceRequirement(ResourceType.AMBULANCE, 1));
                requiredResources.add(new ResourceRequirement(ResourceType.POLICE_VEHICLE, 1));
            }
            case BUILDING_COLLAPSE -> {
                requiredResources.add(new ResourceRequirement(ResourceType.AMBULANCE, 2));
                requiredResources.add(new ResourceRequirement(ResourceType.FIRE_TRUCK, 1));
                requiredResources.add(new ResourceRequirement(ResourceType.RESCUE_TEAM, 3));
            }
            case FLOOD -> {
                requiredResources.add(new ResourceRequirement(ResourceType.AMBULANCE, 2));
                requiredResources.add(new ResourceRequirement(ResourceType.FIRE_TRUCK, 1));
                requiredResources.add(new ResourceRequirement(ResourceType.RESCUE_TEAM, 2));
            }
            case INDUSTRIAL_ACCIDENT -> {
                requiredResources.add(new ResourceRequirement(ResourceType.AMBULANCE, 2));
                requiredResources.add(new ResourceRequirement(ResourceType.FIRE_TRUCK, 2));
                requiredResources.add(new ResourceRequirement(ResourceType.RESCUE_TEAM, 2));
            }
        }
    }

    public List<ResourceRequirement> getRequiredResources() { return new ArrayList<>(requiredResources); }
    public void addAssignedVehicle(VehiclesAssignment assignment) { assignedVehicles.add(assignment); }
    public Set<VehiclesAssignment> getAssignedVehicles() { return new HashSet<>(assignedVehicles); }
    public boolean isResolved() { return status == IncidentStatus.RESOLVED; }
    public void setResolved(LocalDateTime resolutionTime) { this.resolutionTime = resolutionTime; this.status = IncidentStatus.RESOLVED; }

    public long getResponseTimeMinutes() {
        if (creationTime == null) return 0;
        LocalDateTime endTime = resolutionTime != null ? resolutionTime : LocalDateTime.now();
        return java.time.Duration.between(creationTime, endTime).toMinutes();
    }

    public int getTotalAssigned() {
        return assignedVehicles.stream().mapToInt(VehiclesAssignment::getResourceCount).sum();
    }

    @Override public int compareTo(Incident other) {
        // Compare by deadline first, then severity
        int deadlineCompare = this.deadline.compareTo(other.deadline);
        if (deadlineCompare != 0) return deadlineCompare;
        return other.severity.ordinal() - this.severity.ordinal();
    }

    @Override public String toString() {
        return "Incident#" + id + "[" + type + ", " + severity + ", @" + location.id + "]";
    }
    @Override public boolean equals(Object o) { return o instanceof Incident i && id.equals(i.id); }
    @Override public int hashCode() { return id.hashCode(); }
}