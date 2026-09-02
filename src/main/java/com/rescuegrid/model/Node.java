package com.rescuegrid.model;

import java.util.ArrayList;
import java.util.List;

public class Node {
    public final String id;
    public final String name;
    public final NodeType type;
    public final double x;
    public final double y;
    private final List<Road> roads = new ArrayList<>();
    private int emergencyBedCount = 0;
    private int icuBedCount = 0;
    private int doctors = 0;
    private int operatingRooms = 0;
    private int currentPatients = 0;

    public Node(String id, String name, NodeType type, double x, double y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public List<Road> getRoads() { return roads; }
    public void addRoad(Road road) { roads.add(road); }

    public int getEmergencyBedCount() { return emergencyBedCount; }
    public void setEmergencyBedCount(int emergencyBedCount) { this.emergencyBedCount = emergencyBedCount; }
    public int getIcuBedCount() { return icuBedCount; }
    public void setIcuBedCount(int icuBedCount) { this.icuBedCount = icuBedCount; }
    public int getDoctors() { return doctors; }
    public void setDoctors(int doctors) { this.doctors = doctors; }
    public int getOperatingRooms() { return operatingRooms; }
    public void setOperatingRooms(int operatingRooms) { this.operatingRooms = operatingRooms; }
    public int getCurrentPatients() { return currentPatients; }
    public void setCurrentPatients(int currentPatients) { this.currentPatients = currentPatients; }

    public int getAvailableBeds() { return emergencyBedCount - currentPatients; }
    public int getAvailableIcu() { return icuBedCount; }
    public boolean hasCapacity() { return currentPatients < emergencyBedCount; }

    public double distanceTo(Node other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override public String toString() { return id + " (" + name + ") [" + type + "]"; }
    @Override public boolean equals(Object o) { if (!(o instanceof Node)) return false; return id.equals(((Node) o).id); }
    @Override public int hashCode() { return id.hashCode(); }
}
