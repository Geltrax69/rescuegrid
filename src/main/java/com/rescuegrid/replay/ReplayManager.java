package com.rescuegrid.replay;

import com.rescuegrid.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ReplayManager {
    private final List<RecordedEvent> recordedEvents = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void startRecording(EventBus eventBus) {
        for (EventType type : EventType.values()) {
            eventBus.subscribe(type, e -> recordEvent(type, e));
        }
    }

    private void recordEvent(EventType type, Event event) {
        String timestamp = LocalDateTime.now().format(formatter);
        String details = buildEventDetails(type, event);
        recordedEvents.add(new RecordedEvent(timestamp, type.toString(), details));
    }

    private String buildEventDetails(EventType type, Event event) {
        StringBuilder sb = new StringBuilder();
        Object incident = event.get("incident");
        if (incident != null) {
            sb.append("incident=").append(incident.toString());
        }
        Object vehicle = event.get("vehicle");
        if (vehicle != null) {
            sb.append(" vehicle=").append(vehicle.toString());
        }
        Object road = event.get("road");
        if (road != null) {
            sb.append(" road=").append(road.toString());
        }
        Object hospital = event.get("hospital");
        if (hospital != null) {
            sb.append(" hospital=").append(hospital.toString());
        }
        return sb.toString();
    }

    public void saveToFile(String path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("# RescueGrid Simulation Replay");
            writer.println("# Recorded " + recordedEvents.size() + " events");
            writer.println("# Format: timestamp | type | details");
            for (RecordedEvent re : recordedEvents) {
                writer.println(re.timestamp + " | " + re.type + " | " + re.details);
            }
        }
    }

    public void replay(java.util.function.Consumer<String> consumer) {
        for (RecordedEvent re : recordedEvents) {
            try {
                consumer.accept(re.timestamp + " " + re.type + ": " + re.details);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public int getEventCount() { return recordedEvents.size(); }

    private record RecordedEvent(String timestamp, String type, String details) {}
}
