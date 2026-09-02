package com.rescuegrid.simulation;

import com.rescuegrid.model.*;
import java.util.Random;

public class CityBuilder {
    public static City build() {
        City city = new City();

        // Create nodes - a 6x4 grid city
        // H = Hospital, F = Fire station, P = Police, W = Warehouse
        // I = Intersection, R = Residential
        Node[][] grid = new Node[6][4];

        String[][] layout = {
            {"H", "I", "I", "F"},
            {"I", "R", "I", "I"},
            {"W", "I", "P", "I"},
            {"I", "I", "I", "H"},
            {"R", "I", "I", "I"},
            {"I", "W", "F", "I"}
        };

        String[] names = {
            "Central Hospital", "North Hospital", "South Hospital", "West Hospital", "East Hospital",
            "Central Fire", "North Fire", "South Fire", "East Fire",
            "Central Police", "North Police", "South Police", "East Police",
            "Central Warehouse", "North Warehouse", "South Warehouse", "East Warehouse"
        };

        int nameIdx = 0;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                String typeCode = layout[y][x];
                NodeType type = switch (typeCode) {
                    case "H" -> NodeType.HOSPITAL;
                    case "F" -> NodeType.FIRE_STATION;
                    case "P" -> NodeType.POLICE_STATION;
                    case "W" -> NodeType.WAREHOUSE;
                    case "R" -> NodeType.RESIDENTIAL_AREA;
                    default -> NodeType.INTERSECTION;
                };
                String name = nameIdx < names.length ? names[nameIdx++] : typeCode + x + y;
                String id = "N_" + x + "_" + y;
                Node node = new Node(id, name, type, 60 + x * 100, 60 + y * 120);
                city.addNode(node);
                grid[x][y] = node;
            }
        }

        // Connect adjacent nodes with roads
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 6; x++) {
                Node from = grid[x][y];
                if (x + 1 < 6) {
                    Node to = grid[x + 1][y];
                    city.addRoad(new Road("R_" + x + "_" + y + "_h", from, to, 1.0 + Math.random(), 50 + (int)(Math.random() * 30), 50));
                }
                if (y + 1 < 4) {
                    Node to = grid[x][y + 1];
                    city.addRoad(new Road("R_" + x + "_" + y + "_v", from, to, 1.2 + Math.random(), 50 + (int)(Math.random() * 30), 50));
                }
            }
        }

        // Add hospitals
        for (Node node : city.getNodesByType(NodeType.HOSPITAL)) {
            TreatmentCapability cap = switch (new Random().nextInt(4)) {
                case 0 -> TreatmentCapability.BASIC;
                case 1 -> TreatmentCapability.INTERMEDIATE;
                case 2 -> TreatmentCapability.ADVANCED;
                default -> TreatmentCapability.CRITICAL_CARE;
            };
            Hospital hospital = new Hospital("H" + node.id, node.name, node, 30, 10, 8, 4, 100, 200, 1.0, cap);
            city.addHospital(hospital);
        }

        // Add warehouses
        for (Node node : city.getNodesByType(NodeType.WAREHOUSE)) {
            Warehouse warehouse = new Warehouse("W" + node.id, node.name, node, 1000);
            warehouse.addSupply(Supply.SupplyType.MEDICAL_KITS, 500);
            warehouse.addSupply(Supply.SupplyType.BLOOD_UNITS, 200);
            warehouse.addSupply(Supply.SupplyType.OXYGEN, 100);
            warehouse.addSupply(Supply.SupplyType.FOOD, 300);
            warehouse.addSupply(Supply.SupplyType.WATER, 500);
            city.addWarehouse(warehouse);
        }

        // Add vehicles: 20 ambulances, 10 fire trucks, 15 police, 12 rescue teams
        Node[] intersections = city.getNodesByType(NodeType.INTERSECTION).toArray(new Node[0]);

        for (int i = 0; i < 20; i++) {
            Node loc = intersections[i % intersections.length];
            Ambulance a = Ambulance.create("A" + (i + 1));
            a.location = loc;
            city.addAmbulance(a);
        }

        for (int i = 0; i < 10; i++) {
            Node loc = intersections[(i + 3) % intersections.length];
            FireTruck f = FireTruck.create("F" + (i + 1));
            f.location = loc;
            city.addFireTruck(f);
        }

        for (int i = 0; i < 15; i++) {
            Node loc = intersections[(i + 5) % intersections.length];
            Vehicle p = new PoliceVehicleImpl("P" + (i + 1));
            p.location = loc;
            city.addPoliceVehicle(p);
        }

        for (int i = 0; i < 12; i++) {
            Node loc = intersections[(i + 7) % intersections.length];
            Vehicle t = new RescueTeamImpl("T" + (i + 1));
            t.location = loc;
            city.addRescueTeam(t);
        }

        return city;
    }
}

class PoliceVehicleImpl extends Vehicle {
    public PoliceVehicleImpl(String id) { super(id, ResourceType.POLICE_VEHICLE, 2); }
    @Override protected double getDefaultSpeed() { return 90.0; }
}

class RescueTeamImpl extends Vehicle {
    public RescueTeamImpl(String id) { super(id, ResourceType.RESCUE_TEAM, 5); }
    @Override protected double getDefaultSpeed() { return 70.0; }
}
