# RescueGrid - Intelligent Emergency Response & Resource Allocation System

A complete Java desktop simulation for emergency response coordination across a city.

## Features

- **City Simulation**: Weighted graph representing a city with intersections, hospitals, fire stations, police stations, warehouses, and residential areas
- **Emergency Incidents**: Continuously generated medical emergencies, fires, accidents, building collapses, floods, and industrial accidents
- **Resource Management**: Ambulances (20), Fire Trucks (10), Police Vehicles (15), Rescue Teams (12)
- **Smart Resource Allocation**: Priority-based allocation considering severity, distance, response time, capability, and workload
- **Hospital Allocation**: Intelligent hospital selection based on capacity, distance, and specialty
- **Dynamic Routing**: Dijkstra and A* pathfinding algorithms with real-time traffic updates
- **Traffic Simulation**: Dynamic traffic levels affecting vehicle speed and routing
- **Failure Injection**: Test system resilience with vehicle breakdowns, road closures, hospital overloads
- **Mass Casualty Events**: Handle scenarios with 50-200 affected people
- **Supply Chain Management**: Medical kits, blood units, oxygen, food, water allocation
- **Event-Driven Architecture**: Custom EventBus for loose coupling between components
- **Concurrent Resource Allocation**: Thread-safe allocation preventing race conditions
- **Benchmarking**: Compare Dijkstra vs A* routing algorithms
- **Predictive Positioning**: Proactive resource placement based on historical incident data
- **Simulation Replay**: Record and replay simulation events

## Architecture

```
model/           - Data models (City, Node, Road, Vehicle, Incident, Hospital, etc.)
algorithm/        - Routing algorithms (Dijkstra, A*)
engine/           - Core engines (Resource Allocation, Hospital, Supply, Traffic)
event/            - EventBus and event types
simulation/       - Simulation engine and city builder
metrics/          - Metrics collection and reporting
ui/               - Swing/AWT dashboard components
benchmark/        - Algorithm benchmarking
predictive/       - Predictive positioning
scenario/         - Scenario management
replay/           - Simulation replay
```

## Key Algorithms

### Dijkstra's Algorithm
- O((V + E) log V) time complexity
- Guarantees shortest path in weighted graphs
- Used as baseline for comparison

### A* Algorithm
- Uses heuristic (straight-line distance) to guide search
- Typically faster than Dijkstra for point-to-point routing
- Optimal when heuristic is admissible

### Resource Allocation
- Priority queue based on severity and deadline
- Score-based resource selection:
  - severityWeight + distanceWeight + responseTimeWeight + capabilityWeight + workloadWeight
- Thread-safe using ReentrantReadWriteLock

### Hospital Selection
- Considers: distance, available beds, ICU availability, specialty match, traffic conditions
- Prevents race conditions using ReentrantLock

## Threading Model

- **EventBus**: Dedicated processor thread for event distribution
- **SimulationEngine**: Scheduled executor for time-step updates
- **IncidentGenerator**: Background thread generating incidents
- **TrafficManager**: Periodic traffic simulation
- **MetricsManager**: Real-time metrics updates
- **CityPanel**: Swing EDT for UI updates

## Race Condition Prevention

- `ReentrantReadWriteLock` for resource allocation
- `ReentrantLock` for hospital bed management
- `AtomicInteger`/`AtomicReference` for metrics
- `ConcurrentHashMap` for concurrent access
- `CopyOnWriteArrayList` for incident history

## Build & Run

```bash
./build.sh
```

Or manually:
```bash
mkdir -p out
find src/main/java -name "*.java" | xargs javac -d out -sourcepath src/main/java
java -cp out com.rescuegrid.ui.DashboardFrame
```

## UI Components

- **City Map**: Custom JPanel rendering the city graph
- **Control Panel**: Start/Pause/Resume/Reset simulation
- **Failure Injection**: Test buttons for various failure scenarios
- **Metrics Dashboard**: Real-time statistics display
- **Event Log**: Live event stream

## Failure Scenarios

- Vehicle Breakdown: Resource becomes unavailable mid-transit
- Road Accident: Traffic congestion increases
- Hospital Full: No capacity for new patients
- Comm Failure: System operates in degraded mode
- Mass Casualty: 50-200 simultaneous patients

## Scenarios

1. **Normal Day**: Regular city operations
2. **Rush Hour**: High traffic across the city
3. **Major Accident**: Traffic disruption and road closures
4. **Hospital Overload**: All hospitals at capacity
5. **Mass Casualty**: Large-scale emergency
6. **Flood**: Multiple roads impassable
7. **City-Wide Emergency**: Total system stress test
8. **Resource Shortage**: Limited vehicles available

## Performance Considerations

- City graph: 24 nodes, 35+ roads
- Max concurrent incidents: Limited by resources
- Event queue: 10,000 event buffer
- UI refresh: 1 second intervals

## Design Patterns Used

- **Strategy Pattern**: Router interface for Dijkstra/A* swapping
- **Observer Pattern**: EventBus for event-driven communication
- **Factory Pattern**: CityBuilder for creating vehicles/hospitals
- **Singleton Pattern**: EventBus shared across components

## Technical Stack

- Java 17+
- Swing/AWT for UI
- Java Collections Framework
- Java Concurrency Utilities
- No external dependencies

## Future Enhancements

- Distributed architecture with Kafka for event streaming
- Redis for real-time state caching
- PostgreSQL for historical data persistence
- WebSocket for real-time dashboard updates
- REST API for external integrations
- Machine learning for incident prediction
- Multi-city coordination
- Real GPS integration
