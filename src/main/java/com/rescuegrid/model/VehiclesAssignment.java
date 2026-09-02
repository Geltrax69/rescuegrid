package com.rescuegrid.model;

public class VehiclesAssignment {
    public final ResourceType type;
    public final String vehicleId;
    public final int count;
    public VehiclesAssignment(ResourceType type, String vehicleId, int count) {
        this.type = type;
        this.vehicleId = vehicleId;
        this.count = count;
    }
    public ResourceType getType() { return type; }
    public String getVehicleId() { return vehicleId; }
    public int getResourceCount() { return count; }
}
