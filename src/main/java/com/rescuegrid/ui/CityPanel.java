package com.rescuegrid.ui;

import com.rescuegrid.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CityPanel extends JPanel {
    private final City city;
    private Map<String, Vehicle> vehicleLookup;
    private Map<String, Hospital> hospitalLookup;
    private Map<String, Incident> activeIncidents;
    private Object hoveredObject;

    public CityPanel(City city) {
        this.city = city;
        this.vehicleLookup = new HashMap<>();
        this.hospitalLookup = new HashMap<>();
        this.activeIncidents = new HashMap<>();
        setBackground(new Color(245, 245, 250));
        setPreferredSize(new Dimension(800, 600));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void updateVehicles() {
        vehicleLookup.clear();
        for (Vehicle v : city.getAllVehicles()) vehicleLookup.put(v.id, v);
    }

    public void updateHospitals() {
        hospitalLookup.clear();
        for (Hospital h : city.getAllHospitals()) hospitalLookup.put(h.id, h);
    }

    public void updateIncidents(java.util.List<Incident> incidents) {
        activeIncidents.clear();
        for (Incident i : incidents) {
            if (i.status != IncidentStatus.RESOLVED) activeIncidents.put(i.id, i);
        }
    }

    private void handleClick(int x, int y) {
        for (Vehicle v : vehicleLookup.values()) {
            if (v.location != null && near(x, y, v.location.x, v.location.y)) {
                showVehicleInfo(v);
                return;
            }
        }
        for (Hospital h : hospitalLookup.values()) {
            if (near(x, y, h.location.x, h.location.y)) {
                showHospitalInfo(h);
                return;
            }
        }
    }

    private boolean near(int x1, int y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2) < 20;
    }

    private void showVehicleInfo(Vehicle v) {
        JOptionPane.showMessageDialog(this,
            String.format("Vehicle: %s\nType: %s\nStatus: %s\nLocation: %s\nDestination: %s\nSpeed: %.0f km/h",
                v.id, v.resourceType, v.assignment, v.location, v.destination, v.speed),
            "Vehicle Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHospitalInfo(Hospital h) {
        JOptionPane.showMessageDialog(this,
            String.format("Hospital: %s\nBeds: %d/%d\nICU: %d/%d\nDoctors: %d\nOR: %d",
                h.name, h.getAvailableBeds(), h.emergencyBeds, h.getAvailableIcu(), h.icuBeds, h.doctors, h.operatingRooms),
            "Hospital Info", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw roads
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(8));
        for (Road road : city.getAllRoads()) {
            if (road.isOpen()) {
                g2d.setColor(getTrafficColor(road.getTrafficLevel()));
                g2d.drawLine((int)road.from.x, (int)road.from.y, (int)road.to.x, (int)road.to.y);
                g2d.setStroke(new BasicStroke(8));
            } else {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
                g2d.drawLine((int)road.from.x, (int)road.from.y, (int)road.to.x, (int)road.to.y);
                g2d.setStroke(new BasicStroke(8));
            }
        }

        // Draw nodes
        for (Node node : city.getAllNodes()) {
            int x = (int) node.x;
            int y = (int) node.y;
            int size = 20;

            Color color = switch (node.type) {
                case HOSPITAL -> Color.RED;
                case FIRE_STATION -> Color.ORANGE;
                case POLICE_STATION -> Color.BLUE;
                case WAREHOUSE -> new Color(139, 69, 19);
                case RESIDENTIAL_AREA -> Color.GREEN;
                default -> Color.DARK_GRAY;
            };

            g2d.setColor(color);
            g2d.fillOval(x - size/2, y - size/2, size, size);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(x - size/2, y - size/2, size, size);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.drawString(node.id, x - 12, y - 14);
        }

        // Draw incidents
        for (Incident incident : activeIncidents.values()) {
            int x = (int) incident.location.x;
            int y = (int) incident.location.y;
            Color color = switch (incident.severity) {
                case CRITICAL -> Color.RED;
                case HIGH -> new Color(255, 140, 0);
                case MEDIUM -> Color.YELLOW;
                default -> Color.LIGHT_GRAY;
            };
            g2d.setColor(color);
            g2d.fillRect(x - 5, y - 5, 10, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x - 5, y - 5, 10, 10);
        }

        // Draw vehicles
        for (Vehicle v : vehicleLookup.values()) {
            if (v.location == null) continue;
            int x = (int) v.location.x;
            int y = (int) v.location.y;
            int size = 8;
            Color color = switch (v.resourceType) {
                case AMBULANCE -> Color.PINK;
                case FIRE_TRUCK -> Color.RED;
                case POLICE_VEHICLE -> Color.BLUE;
                case RESCUE_TEAM -> new Color(0, 150, 0);
                default -> Color.GRAY;
            };
            g2d.setColor(color);
            g2d.fillRect(x - size/2, y - size/2, size, size);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x - size/2, y - size/2, size, size);
        }
    }

    private Color getTrafficColor(TrafficLevel level) {
        return switch (level) {
            case LOW -> new Color(200, 200, 200);
            case MEDIUM -> new Color(255, 255, 150);
            case HIGH -> new Color(255, 180, 100);
            case VERY_HIGH -> new Color(255, 100, 100);
        };
    }
}
