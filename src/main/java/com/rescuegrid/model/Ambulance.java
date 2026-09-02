package com.rescuegrid.model;

public class Ambulance extends Vehicle {
    public final int medicalKitsCapacity;
    public final int oxygenTanksCapacity;

    public Ambulance(String id, int capacity, int medicalKits, int oxygenTanks) {
        super(id, ResourceType.AMBULANCE, capacity);
        this.medicalKitsCapacity = medicalKits;
        this.oxygenTanksCapacity = oxygenTanks;
    }

    @Override
    protected double getDefaultSpeed() { return 80.0; }

    public static Ambulance create(String id) {
        return new Ambulance(id, 4, 5, 2);
    }
}