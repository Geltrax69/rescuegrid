package com.rescuegrid.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Hospital {
    public final String id;
    public final String name;
    public final Node location;
    public final int emergencyBeds;
    public final int icuBeds;
    public final int doctors;
    public final int operatingRooms;
    public final int bloodUnits;
    public final int medicalSupplies;
    public final double basePriority;
    public final TreatmentCapability capability;
    private final AtomicInteger occupiedEmergencyBeds = new AtomicInteger(0);
    private final AtomicInteger occupiedIcuBeds = new AtomicInteger(0);
    private final AtomicInteger treatedPatients = new AtomicInteger(0);
    private final AtomicInteger patientsWaiting = new AtomicInteger(0);
    private final AtomicInteger bloodStock = new AtomicInteger(0);

    public Hospital(String id, String name, Node location, int emergencyBeds, int icuBeds, int doctors,
                    int operatingRooms, int bloodUnits, int medicalSupplies, double basePriority, TreatmentCapability capability) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.emergencyBeds = emergencyBeds;
        this.icuBeds = icuBeds;
        this.doctors = doctors;
        this.operatingRooms = operatingRooms;
        this.bloodUnits = bloodUnits;
        this.medicalSupplies = medicalSupplies;
        this.basePriority = basePriority;
        this.capability = capability;
        this.bloodStock.set(bloodUnits);
    }

    public boolean hasEmergencyCapacity() { return occupiedEmergencyBeds.get() < emergencyBeds; }
    public boolean hasIcuCapacity() { return occupiedIcuBeds.get() < icuBeds; }
    public boolean hasDoctor() { return doctors > 0; }
    public boolean hasOperatingRoom() { return operatingRooms > 0; }
    public int getAvailableBeds() { return emergencyBeds - occupiedEmergencyBeds.get(); }
    public int getAvailableIcu() { return icuBeds - occupiedIcuBeds.get(); }
    public int getOccupiedEmergencyBeds() { return occupiedEmergencyBeds.get(); }
    public int getOccupiedIcuBeds() { return occupiedIcuBeds.get(); }
    public int getBloodStock() { return bloodStock.get(); }
    public int getMedicalSupplies() { return medicalSupplies; }
    public void setEmergencyBedCount(int count) { /* failure simulation */ }

    public synchronized boolean admitPatient(boolean requiresIcu) {
        if (!hasEmergencyCapacity()) return false;
        if (requiresIcu && !hasIcuCapacity()) return false;
        if (!hasDoctor()) return false;
        if (!hasOperatingRoom()) return false;

        occupiedEmergencyBeds.incrementAndGet();
        if (requiresIcu) occupiedIcuBeds.incrementAndGet();
        treatedPatients.incrementAndGet();
        return true;
    }

    public synchronized void dischargePatient(boolean wasIcu) {
        if (occupiedEmergencyBeds.get() > 0) occupiedEmergencyBeds.decrementAndGet();
        if (wasIcu && occupiedIcuBeds.get() > 0) occupiedIcuBeds.decrementAndGet();
    }

    public synchronized void addBloodUnits(int amount) { bloodStock.addAndGet(amount); }
    public synchronized boolean useBloodUnits(int amount) {
        if (bloodStock.get() >= amount) {
            bloodStock.addAndGet(-amount);
            return true;
        }
        return false;
    }

    public synchronized int getPatientsWaiting() { return patientsWaiting.get(); }
    public synchronized void addWaitingPatient() { patientsWaiting.incrementAndGet(); }
    public synchronized void removeWaitingPatient() { if (patientsWaiting.get() > 0) patientsWaiting.decrementAndGet(); }

    @Override public String toString() {
        return name + "[beds=" + getAvailableBeds() + "/" + emergencyBeds + ", icu=" + getAvailableIcu() + "/" + icuBeds + "]";
    }
}
