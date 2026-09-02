package com.rescuegrid.ui;

import com.rescuegrid.model.*;
import com.rescuegrid.simulation.*;
import com.rescuegrid.event.*;
import com.rescuegrid.metrics.*;
import com.rescuegrid.engine.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class DashboardFrame extends JFrame {
    private final City city;
    private final EventBus eventBus;
    private final SimulationEngine simulationEngine;
    private final MetricsManager metricsManager;
    private final CityPanel cityPanel;
    private final JTextArea eventLog;
    private final JLabel[] metricsLabels;
    private final DefaultListModel<String> incidentListModel;

    public DashboardFrame() {
        super("RescueGrid - Intelligent Emergency Response & Resource Allocation System");

        this.city = CityBuilder.build();
        this.eventBus = new EventBus();
        this.metricsManager = new MetricsManager(eventBus);
        this.simulationEngine = new SimulationEngine(city, eventBus);

        eventBus.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1400, 900);

        // Initialize UI
        cityPanel = new CityPanel(city);
        cityPanel.updateVehicles();
        cityPanel.updateHospitals();

        // Control Panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setPreferredSize(new Dimension(280, 0));

        // Simulation controls
        JPanel simControls = new JPanel(new GridLayout(1, 4, 5, 5));
        simControls.setBorder(BorderFactory.createTitledBorder("Simulation"));
        JButton startBtn = new JButton("Start");
        JButton pauseBtn = new JButton("Pause");
        JButton resumeBtn = new JButton("Resume");
        JButton resetBtn = new JButton("Reset");

        startBtn.addActionListener(e -> {
            simulationEngine.start();
            startBtn.setEnabled(false);
            resumeBtn.setEnabled(true);
        });
        pauseBtn.addActionListener(e -> {
            simulationEngine.pause();
            pauseBtn.setEnabled(false);
            resumeBtn.setEnabled(true);
        });
        resumeBtn.addActionListener(e -> {
            simulationEngine.resume();
            pauseBtn.setEnabled(true);
        });
        resumeBtn.setEnabled(false);

        resetBtn.addActionListener(e -> {
            simulationEngine.stop();
            startBtn.setEnabled(true);
            pauseBtn.setEnabled(true);
            resumeBtn.setEnabled(false);
        });

        simControls.add(startBtn);
        simControls.add(pauseBtn);
        simControls.add(resumeBtn);
        simControls.add(resetBtn);

        // Failure injection
        JPanel failurePanel = new JPanel(new GridLayout(4, 2, 5, 5));
        failurePanel.setBorder(BorderFactory.createTitledBorder("Failure Injection"));

        JButton breakdownBtn = new JButton("Vehicle Breakdown");
        JButton roadCloseBtn = new JButton("Road Accident");
        JButton hospitalFullBtn = new JButton("Hospital Full");
        JButton massCasualtyBtn = new JButton("Mass Casualty");
        JButton commFailureBtn = new JButton("Comm Failure");
        JButton resourceFailureBtn = new JButton("Resource Failure");
        JButton trafficExplosionBtn = new JButton("Traffic Explosion");
        JButton newCriticalBtn = new JButton("New Critical");

        breakdownBtn.addActionListener(e -> injectFailure("VEHICLE_BREAKDOWN"));
        roadCloseBtn.addActionListener(e -> injectFailure("ROAD_ACCIDENT"));
        hospitalFullBtn.addActionListener(e -> injectFailure("HOSPITAL_FULL"));
        massCasualtyBtn.addActionListener(e -> injectFailure("MASS_CASUALTY"));
        commFailureBtn.addActionListener(e -> injectFailure("COMM_FAILURE"));
        resourceFailureBtn.addActionListener(e -> injectFailure("RESOURCE_FAILURE"));
        trafficExplosionBtn.addActionListener(e -> injectFailure("TRAFFIC_EXPLOSION"));
        newCriticalBtn.addActionListener(e -> injectFailure("NEW_CRITICAL"));

        failurePanel.add(breakdownBtn);
        failurePanel.add(roadCloseBtn);
        failurePanel.add(hospitalFullBtn);
        failurePanel.add(massCasualtyBtn);
        failurePanel.add(commFailureBtn);
        failurePanel.add(resourceFailureBtn);
        failurePanel.add(trafficExplosionBtn);
        failurePanel.add(newCriticalBtn);

        // Load testing
        JPanel loadPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        loadPanel.setBorder(BorderFactory.createTitledBorder("Load Test"));
        loadPanel.add(new JButton("100 Incidents"));
        loadPanel.add(new JButton("1,000 Incidents"));
        loadPanel.add(new JButton("10,000 Incidents"));
        loadPanel.add(new JButton("50,000 Events"));

        // Incident list
        incidentListModel = new DefaultListModel<>();
        JList<String> incidentList = new JList<>(incidentListModel);
        JScrollPane incidentScroll = new JScrollPane(incidentList);
        incidentScroll.setBorder(BorderFactory.createTitledBorder("Active Incidents"));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(simControls, BorderLayout.NORTH);
        leftPanel.add(failurePanel, BorderLayout.CENTER);
        leftPanel.add(loadPanel, BorderLayout.SOUTH);

        // Metrics Panel
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Real-Time Metrics"));

        JPanel metricsGrid = new JPanel(new GridLayout(6, 2, 10, 5));
        metricsLabels = new JLabel[12];
        String[] metricNames = {
            "Total Incidents:", "Active Incidents:", "Critical:", "Resolved:",
            "Available Ambulances:", "Busy Ambulances:", "Avg Response:", "Max Response:",
            "Hospitals:", "Resource Utilization:", "Patients Treated:", "Patients Waiting:"
        };

        for (int i = 0; i < 12; i++) {
            metricsGrid.add(new JLabel(metricNames[i]));
            metricsLabels[i] = new JLabel("0");
            metricsLabels[i].setFont(new Font("Arial", Font.BOLD, 12));
            metricsGrid.add(metricsLabels[i]);
        }

        metricsPanel.add(metricsGrid, BorderLayout.NORTH);

        // Event Log
        eventLog = new JTextArea(15, 40);
        eventLog.setEditable(false);
        eventLog.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane logScroll = new JScrollPane(eventLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.add(metricsPanel, BorderLayout.NORTH);
        rightPanel.add(logScroll, BorderLayout.CENTER);

        // Main content
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        JSplitPane fullSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, incidentScroll);
        fullSplit.setDividerLocation(350);

        add(fullSplit, BorderLayout.CENTER);
        add(cityPanel, BorderLayout.CENTER);

        // Subscribe to events for logging
        subscribeToEvents();

        // Start metrics update timer
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> updateMetrics());
        timer.start();

        setLocationRelativeTo(null);
        setVisible(true);

        logEvent("RescueGrid initialized. Press Start to begin simulation.");
    }

    private void injectFailure(String type) {
        logEvent("FAILURE INJECTED: " + type);
        com.rescuegrid.event.Event failureEvent = new com.rescuegrid.event.Event(EventType.FAILURE_INJECTED).with("type", type);
        eventBus.publish(failureEvent);
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.INCIDENT_CREATED, e -> {
            Incident inc = e.get("incident");
            if (inc != null) {
                SwingUtilities.invokeLater(() -> {
                    incidentListModel.addElement(String.format("[%s] %s %s @ %s",
                        inc.severity, inc.type, inc.id, inc.location.id));
                    if (incidentListModel.getSize() > 100) incidentListModel.removeElementAt(0);
                });
                logEvent(String.format("INCIDENT #%s: %s (Severity: %s, People: %d)",
                    inc.id, inc.type, inc.severity, inc.peopleAffected));
            }
        });

        eventBus.subscribe(EventType.VEHICLE_DISPATCHED, e -> {
            Vehicle v = e.get("vehicle");
            if (v != null) {
                logEvent(String.format("DISPATCHED: %s -> %s", v.id, v.destination));
            }
        });

        eventBus.subscribe(EventType.INCIDENT_RESOLVED, e -> {
            Incident inc = e.get("incident");
            if (inc != null) {
                logEvent(String.format("RESOLVED: %s (Response time: %d min)", inc.id, inc.getResponseTimeMinutes()));
            }
        });

        eventBus.subscribe(EventType.ROAD_CLOSED, e -> {
            Road r = e.get("road");
            if (r != null) {
                logEvent(String.format("ROAD CLOSED: %s", r.id));
                cityPanel.repaint();
            }
        });

        eventBus.subscribe(EventType.VEHICLE_FAILED, e -> {
            Vehicle v = e.get("vehicle");
            if (v != null) {
                logEvent(String.format("VEHICLE FAILURE: %s", v.id));
                cityPanel.repaint();
            }
        });

        eventBus.subscribe(EventType.HOSPITAL_FULL, e -> {
            Hospital h = e.get("hospital");
            if (h != null) {
                logEvent(String.format("HOSPITAL FULL: %s", h.name));
            }
        });
    }

    private void updateMetrics() {
        metricsLabels[0].setText(String.valueOf(metricsManager.getTotalIncidents()));
        metricsLabels[1].setText(String.valueOf(metricsManager.getActiveIncidents()));
        metricsLabels[2].setText(String.valueOf(metricsManager.getCriticalIncidents()));
        metricsLabels[3].setText(String.valueOf(metricsManager.getResolvedIncidents()));
        metricsLabels[4].setText(String.valueOf(metricsManager.getAvailableAmbulances()));
        metricsLabels[5].setText(String.valueOf(metricsManager.getBusyAmbulances()));
        metricsLabels[6].setText(String.format("%.1f ms", metricsManager.getAverageResponseTimeMs()));
        metricsLabels[7].setText(String.format("%d ms", metricsManager.getMaxResponseTimeMs()));
        metricsLabels[8].setText(String.valueOf(city.getAllHospitals().size()));
        metricsLabels[9].setText(String.format("%.1f%%", metricsManager.getResourceUtilization()));
        metricsLabels[10].setText(String.valueOf(metricsManager.getPatientsTreated()));
        metricsLabels[11].setText(String.valueOf(metricsManager.getPatientsWaiting()));

        cityPanel.updateVehicles();
        cityPanel.updateHospitals();
    }

    private void logEvent(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            eventLog.append(String.format("[%s] %s\n", timestamp, message));
            eventLog.setCaretPosition(eventLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardFrame());
    }
}
