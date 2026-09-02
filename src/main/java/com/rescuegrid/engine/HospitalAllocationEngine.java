package com.rescuegrid.engine;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HospitalAllocationEngine {
    private final City city;
    private final EventBus eventBus;
    private final Map<String, Patient> waitingPatients = new ConcurrentHashMap<>();
    private final Map<String, Hospital> hospitalOccupancyTracker = new ConcurrentHashMap<>();
    private final AllocationConfig config;
    private final ReentrantLock hospitalLock = new ReentrantLock();

    public HospitalAllocationEngine(City city, EventBus eventBus, AllocationConfig config) {
        this.city = city;
        this.eventBus = eventBus;
        this.config = config;
        initializeOccupancyTrackers();
        subscribeToEvents();
    }

    private void initializeOccupancyTrackers() {
        for (Hospital hospital : city.getAllHospitals()) {
            hospitalOccupancyTracker.put(hospital.id, hospital);
        }
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.VEHICLE_ARRIVED, this::onVehicleArrived);
        eventBus.subscribe(EventType.PATIENT_ADMITTED, this::onPatientAdmitted);
        eventBus.subscribe(EventType.INCIDENT_RESOLVED, this::onIncidentResolved);
        eventBus.subscribe(EventType.MASS_CASUALTY, this::onMassCasualty);
    }

    private void onVehicleArrived(Event event) {
        Vehicle vehicle = event.get("vehicle");
        Incident incident = event.get("incident");
        if (vehicle != null && incident != null) {
            // Assign hospital based on incident severity and patient needs
            assignHospital(incident, vehicle);
        }
    }

    private void onPatientAdmitted(Event event) {
        Patient patient = event.get("patient");
        Hospital hospital = event.get("hospital");
        if (patient != null && hospital != null) {
            patient.setAdmitted(true);
        }
    }

    private void onIncidentResolved(Event event) {
        Incident incident = event.get("incident");
        incident.getAssignedVehicles().forEach(assignment -> {
            Vehicle vehicle = findVehicleById(assignment.getVehicleId());
            if (vehicle != null) {
                returnVehicleToPool(vehicle);
            }
        });
    }

    private void onMassCasualty(Event event) {
        int patientCount = event.get("patientCount");
        for (int i = 0; i < patientCount; i++) {
            Patient patient = new Patient("P" + i, "MASS");
            patient.setSeverity(Severity.CRITICAL);
            patient.setNeedsIcu(true);
            patient.setNeedsSpecialCare(true);
            waitingPatients.put(patient.id, patient);
        }
    }

    private void assignHospital(Incident incident, Vehicle vehicle) {
        Hospital selected = null;
        double bestScore = Double.MAX_VALUE;
        hospitalLock.lock();
        try {
            for (Hospital hospital : city.getAllHospitals()) {
                double score = calculateHospitalScore(hospital, incident);
                if (score < bestScore && isHospitalAvailable(hospital, incident)) {
                    bestScore = score;
                    selected = hospital;
                }
            }

            if (selected != null) {
                // Admit patients
                boolean admitted = selected.admitPatient(incident.type.equals(IncidentType.BUILDING_COLLAPSE) ||
                        incident.type.equals(IncidentType.INDUSTRIAL_ACCIDENT));
                if (admitted) {
                    Patient patient = new Patient("PAT" + incident.id, incident.id);
                    patient.setIncident(incident);
                    patient.setSeverity(incident.severity);
                    patient.setNeedsIcu(incident.severity == Severity.CRITICAL);

                    waitingPatients.put(patient.id, patient);
                    eventBus.publish(new Event(EventType.PATIENT_ADMITTED)
                        .with("patient", patient)
                        .with("hospital", selected)
                    );

                    // Log assignment
                    System.out.printf("Admitted to %s: Incident %s%n", selected.name, incident.id);
                }
            }
        } finally {
            hospitalLock.unlock();
        }
    }

    private boolean isHospitalAvailable(Hospital hospital, Incident incident) {
        boolean emergencyBedAvailable = hospital.hasEmergencyCapacity();
        boolean icuAvailable = !incident.type.equals(IncidentType.ACCIDENT) &&
                incident.type.equals(IncidentType.MEDICAL_EMERGENCY) ? hospital.hasIcuCapacity() : true;
        boolean doctorAvailable = hospital.hasDoctor();
        boolean operatingRoomAvailable = hospital.hasOperatingRoom();

        return emergencyBedAvailable && icuAvailable && doctorAvailable && operatingRoomAvailable;
    }

    private double calculateHospitalScore(Hospital hospital, Incident incident) {
        double distance = hospital.location.distanceTo(incident.location);
        double capacityScore = 1.0 / Math.max(1.0, hospital.getAvailableBeds());
        double icuScore = incident.severity == Severity.CRITICAL && hospital.getAvailableIcu() > 0 ?
            0.1 : Double.MAX_VALUE;
        double trafficScore = calculateTrafficScore(incident.location);
        double specialtyScore = getSpecialtyScore(hospital, incident);

        double basePriority = hospital.basePriority;
        double weight = config.hospitalWeight;

        return (basePriority * weight) +
               (distance * config.distanceWeight) +
               (capacityScore * config.capacityWeight) +
               (trafficScore * config.trafficWeight) +
               (specialtyScore * config.specialtyWeight);
    }

    private double calculateTrafficScore(Node location) {
        double totalTraffic = 0;
        for (Road road : location.getRoads()) {
            if (road.isOpen()) {
                totalTraffic += road.getCongestionFactor();
            }
        }
        return totalTraffic / Math.max(1.0, location.getRoads().size());
    }

    private double getSpecialtyScore(Hospital hospital, Incident incident) {
        switch (incident.type) {
            case MEDICAL_EMERGENCY:
                return hospital.capability == TreatmentCapability.CRITICAL_CARE ? 0.1 : 1.0;
            case FIRE:
                return hospital.capability == TreatmentCapability.INTERMEDIATE ? 0.2 : 2.0;
            case BUILDING_COLLAPSE:
                return hospital.capability == TreatmentCapability.ADVANCED ? 0.3 : 1.5;
            default:
                return 1.0;
        }
    }

    private Vehicle findVehicleById(String vehicleId) {
        for (Vehicle v : city.getAllVehicles()) {
            if (v.id.equals(vehicleId)) return v;
        }
        return null;
    }

    private void returnVehicleToPool(Vehicle vehicle) {
        vehicle.assignment = Vehicle.AssignmentInfo.NONE;
        vehicle.location = null;
        vehicle.destination = null;
    }

    public int getWaitingPatientsCount() { return waitingPatients.size(); }
    public int getAverageResponseTime() { return 0; }
}

class Patient {
    public final String id;
    public final String incidentId;
    public Severity severity;
    public boolean needsIcu;
    public boolean needsSpecialCare;
    public boolean admitted;
    public Incident incident;

    public Patient(String id, String incidentId) {
        this.id = id;
        this.incidentId = incidentId;
        this.severity = Severity.MEDIUM;
        this.needsIcu = false;
        this.needsSpecialCare = false;
        this.admitted = false;
    }

    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public void setNeedsIcu(boolean needsIcu) { this.needsIcu = needsIcu; }
    public void setNeedsSpecialCare(boolean needsSpecialCare) { this.needsSpecialCare = needsSpecialCare; }
    public void setAdmitted(boolean admitted) { this.admitted = admitted; }

    @Override public String toString() { return "Patient#" + id + " (from " + incidentId + ")"; }
}