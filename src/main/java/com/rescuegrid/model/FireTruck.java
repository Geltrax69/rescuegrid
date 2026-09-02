package com.rescuegrid.model;

public class FireTruck extends Vehicle {
    public FireTruck(String id, int capacity) {
        super(id, ResourceType.FIRE_TRUCK, capacity);
    }
    @Override protected double getDefaultSpeed() { return 75.0; }
    public static FireTruck create(String id) { return new FireTruck(id, 3); }
}