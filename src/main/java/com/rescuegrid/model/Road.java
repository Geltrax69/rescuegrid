package com.rescuegrid.model;

public class Road {
    public final String id;
    public final Node from;
    public final Node to;
    public final double distance; // km
    public final double speedLimit; // km/h
    public final int capacity; // max vehicles
    private int currentVehicles = 0;
    private TrafficLevel trafficLevel = TrafficLevel.LOW;
    private RoadStatus status = RoadStatus.OPEN;

    public Road(String id, Node from, Node to, double distance, double speedLimit, int capacity) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.speedLimit = speedLimit;
        this.capacity = capacity;
    }

    public synchronized void addVehicle() { currentVehicles++; updateTrafficLevel(); }
    public synchronized void removeVehicle() { currentVehicles = Math.max(0, currentVehicles - 1); updateTrafficLevel(); }
    public synchronized int getCurrentVehicles() { return currentVehicles; }

    private void updateTrafficLevel() {
        double ratio = (double) currentVehicles / capacity;
        if (ratio >= 0.9) trafficLevel = TrafficLevel.VERY_HIGH;
        else if (ratio >= 0.7) trafficLevel = TrafficLevel.HIGH;
        else if (ratio >= 0.4) trafficLevel = TrafficLevel.MEDIUM;
        else trafficLevel = TrafficLevel.LOW;
    }

    public synchronized TrafficLevel getTrafficLevel() { return trafficLevel; }
    public synchronized void setTrafficLevel(TrafficLevel level) { this.trafficLevel = level; }
    public synchronized RoadStatus getStatus() { return status; }
    public synchronized void setStatus(RoadStatus status) { this.status = status; }
    public synchronized boolean isOpen() { return status == RoadStatus.OPEN; }
    public synchronized double getEffectiveSpeed() {
        if (!isOpen()) return 0;
        return speedLimit * (1.0 - trafficLevel.getSpeedReduction());
    }
    public synchronized double getTravelTime() {
        if (!isOpen()) return Double.MAX_VALUE;
        return (distance / getEffectiveSpeed()) * 60; // minutes
    }
    public synchronized double getCongestionFactor() { return (double) currentVehicles / capacity; }

    @Override public String toString() { return id + ": " + from.id + " <-> " + to.id + " (" + distance + "km)"; }
    @Override public boolean equals(Object o) { if (!(o instanceof Road)) return false; return id.equals(((Road) o).id); }
    @Override public int hashCode() { return id.hashCode(); }
}
