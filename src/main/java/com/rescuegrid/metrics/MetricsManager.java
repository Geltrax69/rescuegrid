package com.rescuegrid.metrics;

import com.rescuegrid.event.*;
import com.rescuegrid.model.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class MetricsManager {
    private final AtomicInteger totalIncidents = new AtomicInteger(0);
    private final AtomicInteger activeIncidents = new AtomicInteger(0);
    private final AtomicInteger resolvedIncidents = new AtomicInteger(0);
    private final AtomicInteger criticalIncidents = new AtomicInteger(0);
    private final AtomicInteger availableAmbulances = new AtomicInteger(20);
    private final AtomicInteger busyAmbulances = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final AtomicInteger responseCount = new AtomicInteger(0);
    private final AtomicInteger maxResponseTimeMs = new AtomicInteger(0);
    private final AtomicReference<Double> averageRouteTime = new AtomicReference<>(0.0);
    private final AtomicInteger routeCalculations = new AtomicInteger(0);
    private final AtomicInteger patientsTreated = new AtomicInteger(0);
    private final AtomicInteger patientsWaiting = new AtomicInteger(0);
    private final AtomicInteger suppliesDelivered = new AtomicInteger(0);
    private final EventBus eventBus;

    public MetricsManager(EventBus eventBus) {
        this.eventBus = eventBus;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        eventBus.subscribe(EventType.INCIDENT_CREATED, e -> {
            totalIncidents.incrementAndGet();
            activeIncidents.incrementAndGet();
            Incident inc = e.get("incident");
            if (inc != null && inc.severity == Severity.CRITICAL) {
                criticalIncidents.incrementAndGet();
            }
        });
        eventBus.subscribe(EventType.INCIDENT_RESOLVED, e -> {
            activeIncidents.decrementAndGet();
            resolvedIncidents.incrementAndGet();
            Incident inc = e.get("incident");
            if (inc != null && inc.severity == Severity.CRITICAL) {
                criticalIncidents.decrementAndGet();
            }
            if (inc != null) {
                long responseTime = java.time.Duration.between(inc.creationTime, LocalDateTime.now()).toMillis();
                totalResponseTimeMs.addAndGet(responseTime);
                responseCount.incrementAndGet();
                maxResponseTimeMs.updateAndGet(v -> Math.max(v, (int)responseTime));
            }
        });
        eventBus.subscribe(EventType.PATIENT_ADMITTED, e -> {
            patientsTreated.incrementAndGet();
            patientsWaiting.decrementAndGet();
        });
        eventBus.subscribe(EventType.MASS_CASUALTY, e -> {
            Integer count = e.get("patientCount");
            if (count != null) patientsWaiting.addAndGet(count);
        });
        eventBus.subscribe(EventType.SUPPLY_DEPLETED, e -> suppliesDelivered.incrementAndGet());
    }

    public void updateAmbulanceStats(int available, int busy) {
        availableAmbulances.set(available);
        busyAmbulances.set(busy);
    }

    public void recordRouteCalculation(double timeMs, int nodesVisited) {
        routeCalculations.incrementAndGet();
        averageRouteTime.updateAndGet(v -> {
            int n = routeCalculations.get();
            return ((v * (n - 1)) + timeMs) / n;
        });
    }

    public double getResourceUtilization() {
        int total = availableAmbulances.get() + busyAmbulances.get();
        return total > 0 ? (double) busyAmbulances.get() / total * 100 : 0;
    }

    public int getTotalIncidents() { return totalIncidents.get(); }
    public int getActiveIncidents() { return activeIncidents.get(); }
    public int getResolvedIncidents() { return resolvedIncidents.get(); }
    public int getCriticalIncidents() { return criticalIncidents.get(); }
    public int getAvailableAmbulances() { return availableAmbulances.get(); }
    public int getBusyAmbulances() { return busyAmbulances.get(); }
    public double getAverageResponseTimeMs() { return responseCount.get() > 0 ? (double)totalResponseTimeMs.get() / responseCount.get() : 0; }
    public int getMaxResponseTimeMs() { return maxResponseTimeMs.get(); }
    public double getAverageRouteTimeMs() { return averageRouteTime.get(); }
    public int getRouteCalculations() { return routeCalculations.get(); }
    public int getPatientsTreated() { return patientsTreated.get(); }
    public int getPatientsWaiting() { return patientsWaiting.get(); }
    public int getSuppliesDelivered() { return suppliesDelivered.get(); }

    public String getSummary() {
        return String.format("""
            === RESCUEGRID METRICS ===
            Incidents: %d total | %d active | %d resolved | %d critical
            Ambulances: %d available | %d busy
            Response Time: avg=%.1fms | max=%dms
            Routes: %d calculated | avg=%.2fms
            Patients: %d treated | %d waiting
            Supplies Delivered: %d
            Resource Utilization: %.1f%%
            """,
            getTotalIncidents(), getActiveIncidents(), getResolvedIncidents(), getCriticalIncidents(),
            getAvailableAmbulances(), getBusyAmbulances(),
            getAverageResponseTimeMs(), getMaxResponseTimeMs(),
            getRouteCalculations(), getAverageRouteTimeMs(),
            getPatientsTreated(), getPatientsWaiting(),
            getSuppliesDelivered(),
            getResourceUtilization()
        );
    }
}
