package com.rescuegrid.model;

import java.util.*;
import java.util.concurrent.*;

public class City {
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, Road> roads = new ConcurrentHashMap<>();
    private final Map<String, Hospital> hospitals = new ConcurrentHashMap<>();
    private final Map<String, Warehouse> warehouses = new ConcurrentHashMap<>();
    private final List<Ambulance> ambulances = new CopyOnWriteArrayList<>();
    private final List<FireTruck> fireTrucks = new CopyOnWriteArrayList<>();
    private final List<Vehicle> policeVehicles = new CopyOnWriteArrayList<>();
    private final List<Vehicle> rescueTeams = new CopyOnWriteArrayList<>();

    public void addNode(Node node) { nodes.put(node.id, node); }
    public Node getNode(String id) { return nodes.get(id); }
    public Collection<Node> getAllNodes() { return nodes.values(); }
    public List<Node> getNodesByType(NodeType type) {
        return nodes.values().stream().filter(n -> n.type == type).toList();
    }

    public void addRoad(Road road) { roads.put(road.id, road); road.from.addRoad(road); road.to.addRoad(road); }
    public Collection<Road> getAllRoads() { return roads.values(); }
    public Road getRoad(String id) { return roads.get(id); }

    public void addHospital(Hospital hospital) { hospitals.put(hospital.id, hospital); }
    public Hospital getHospital(String id) { return hospitals.get(id); }
    public Collection<Hospital> getAllHospitals() { return hospitals.values(); }

    public void addWarehouse(Warehouse warehouse) { warehouses.put(warehouse.id, warehouse); }
    public Warehouse getWarehouse(String id) { return warehouses.get(id); }
    public Collection<Warehouse> getAllWarehouses() { return warehouses.values(); }

    public void addAmbulance(Ambulance a) { ambulances.add(a); }
    public void addFireTruck(FireTruck f) { fireTrucks.add(f); }
    public void addPoliceVehicle(Vehicle p) { policeVehicles.add(p); }
    public void addRescueTeam(Vehicle t) { rescueTeams.add(t); }
    public List<Ambulance> getAmbulances() { return ambulances; }
    public List<FireTruck> getFireTrucks() { return fireTrucks; }
    public List<Vehicle> getPoliceVehicles() { return policeVehicles; }
    public List<Vehicle> getRescueTeams() { return rescueTeams; }

    public List<? extends Vehicle> getAllVehicles() {
        List<Vehicle> all = new ArrayList<>();
        all.addAll(ambulances);
        all.addAll(fireTrucks);
        all.addAll(policeVehicles);
        all.addAll(rescueTeams);
        return all;
    }
}
