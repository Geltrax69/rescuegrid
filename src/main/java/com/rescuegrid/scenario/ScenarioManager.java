package com.rescuegrid.scenario;

import com.rescuegrid.model.*;
import com.rescuegrid.simulation.*;
import java.util.*;

public class ScenarioManager {
    private final City city;

    public ScenarioManager(City city) {
        this.city = city;
    }

    public void applyScenario(ScenarioType type) {
        switch (type) {
            case NORMAL_DAY -> applyNormalDay();
            case RUSH_HOUR -> applyRushHour();
            case MAJOR_ACCIDENT -> applyMajorAccident();
            case HOSPITAL_OVERLOAD -> applyHospitalOverload();
            case MASS_CASUALTY -> applyMassCasualty();
            case FLOOD -> applyFlood();
            case CITY_WIDE_EMERGENCY -> applyCityWideEmergency();
            case RESOURCE_SHORTAGE -> applyResourceShortage();
        }
    }

    private void applyNormalDay() {
        resetAll();
        setTraffic(0.3);
        System.out.println("Scenario: NORMAL DAY applied");
    }

    private void applyRushHour() {
        resetAll();
        setTraffic(0.85);
        System.out.println("Scenario: RUSH HOUR applied");
    }

    private void applyMajorAccident() {
        resetAll();
        setTraffic(0.5);
        // Close some roads
        int closed = 0;
        for (Road road : city.getAllRoads()) {
            if (Math.random() < 0.1 && closed < 3) {
                road.setStatus(RoadStatus.CLOSED);
                closed++;
            }
        }
        System.out.println("Scenario: MAJOR ACCIDENT applied");
    }

    private void applyHospitalOverload() {
        resetAll();
        for (Hospital hospital : city.getAllHospitals()) {
            hospital.setEmergencyBedCount(0);
        }
        System.out.println("Scenario: HOSPITAL OVERLOAD applied");
    }

    private void applyMassCasualty() {
        resetAll();
        setTraffic(0.6);
        System.out.println("Scenario: MASS CASUALTY applied");
    }

    private void applyFlood() {
        resetAll();
        for (Road road : city.getAllRoads()) {
            if (Math.random() < 0.3) {
                road.setStatus(RoadStatus.CLOSED);
            }
        }
        System.out.println("Scenario: FLOOD applied");
    }

    private void applyCityWideEmergency() {
        resetAll();
        setTraffic(0.9);
        for (Hospital hospital : city.getAllHospitals()) {
            hospital.setEmergencyBedCount(0);
        }
        System.out.println("Scenario: CITY-WIDE EMERGENCY applied");
    }

    private void applyResourceShortage() {
        resetAll();
        for (Ambulance a : city.getAmbulances()) {
            a.assignment = Vehicle.AssignmentInfo.MAINTENANCE;
        }
        System.out.println("Scenario: RESOURCE SHORTAGE applied");
    }

    private void resetAll() {
        for (Road road : city.getAllRoads()) {
            road.setStatus(RoadStatus.OPEN);
        }
        for (Hospital hospital : city.getAllHospitals()) {
            hospital.setEmergencyBedCount(hospital.emergencyBeds);
        }
        for (Vehicle vehicle : city.getAllVehicles()) {
            if (vehicle.assignment == Vehicle.AssignmentInfo.MAINTENANCE) {
                vehicle.assignment = Vehicle.AssignmentInfo.NONE;
            }
        }
    }

    private void setTraffic(double factor) {
        Random random = new Random();
        for (Road road : city.getAllRoads()) {
            int vehicles = (int) (road.capacity * factor * random.nextDouble());
            for (int i = 0; i < vehicles; i++) {
                road.addVehicle();
            }
        }
    }

    public enum ScenarioType {
        NORMAL_DAY, RUSH_HOUR, MAJOR_ACCIDENT, HOSPITAL_OVERLOAD,
        MASS_CASUALTY, FLOOD, CITY_WIDE_EMERGENCY, RESOURCE_SHORTAGE
    }
}
