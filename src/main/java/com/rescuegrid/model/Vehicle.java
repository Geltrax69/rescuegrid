package com.rescuegrid.model;

import java.util.*;
import java.util.concurrent.*;

public abstract class Vehicle implements Comparable<Vehicle> {
    public final String id;
    public final ResourceType resourceType;
    public volatile Node location;
    public volatile Node destination;
    public volatile RoadStatus roadStatus = RoadStatus.OPEN;
    public volatile int currentLoad = 0;
    public volatile int capacity;
    public volatile double speed; // km/h
    public volatile long etaMinutes;
    public volatile AssignmentInfo assignment;
    public volatile java.time.LocalDateTime arrivalTime;

    public enum AssignmentInfo { NONE, ASSIGNED, EN_ROUTE, ON_SCENE, RETURNING, MAINTENANCE }

    public Vehicle(String id, ResourceType type, int capacity) {
        this.id = id;
        this.resourceType = type;
        this.capacity = capacity;
        this.speed = getDefaultSpeed();
    }

    protected abstract double getDefaultSpeed();

    public synchronized double getEtaTo(Node destination) {
        if (location == null || destination == null) return -1;
        double directDistance = location.distanceTo(destination);
        Road road = findDirectRoad(destination);
        if (road != null && road.isOpen()) {
            double travelTimeMinutes = road.getTravelTime();
            return directDistance / Math.max(speed, 1) * 60; // simplified
        }
        return location.distanceTo(destination) / Math.max(speed, 1) * 60;
    }

    private Road findDirectRoad(Node to) {
        for (Road road : location.getRoads()) {
            if (road.to == to || road.from == to) return road;
        }
        return null;
    }

    public void dispatchTo(Node destination) {
        this.destination = destination;
        this.roadStatus = RoadStatus.OPEN;
        this.assignment = AssignmentInfo.EN_ROUTE;
    }

    public synchronized void arriveAt() {
        this.assignment = AssignmentInfo.ON_SCENE;
        this.roadStatus = RoadStatus.OPEN;
        this.destination = null;
    }

    public synchronized void returnToBase() {
        this.assignment = AssignmentInfo.RETURNING;
        this.roadStatus = RoadStatus.OPEN;
        this.location = null;
        this.destination = null;
    }

    @Override public int compareTo(Vehicle other) {
        return this.id.compareTo(other.id);
    }
    @Override public String toString() { return resourceType + "#" + id + " [loc=" + location + ", load=" + currentLoad + "/" + capacity + "]"; }
    @Override public boolean equals(Object o) { return o instanceof Vehicle v && id.equals(v.id); }
    @Override public int hashCode() { return id.hashCode(); }
}