package com.rescuegrid.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Warehouse {
    public final String id;
    public final String name;
    public final Node location;
    private final Map<Supply.SupplyType, Supply> supplies = new ConcurrentHashMap<>();
    private final int maxCapacity;

    public Warehouse(String id, String name, Node location, int maxCapacity) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.maxCapacity = maxCapacity;
    }

    public void addSupply(Supply.SupplyType type, int amount) {
        supplies.computeIfPresent(type, (k, v) -> new Supply(type, v.quantity + amount));
    }
    public boolean hasSupply(Supply.SupplyType type, int amount) {
        Supply s = supplies.get(type);
        return s != null && s.quantity >= amount;
    }
    public boolean consumeSupply(Supply.SupplyType type, int amount) {
        synchronized (this) {
            Supply s = supplies.get(type);
            if (s != null && s.quantity >= amount) {
                s.quantity -= amount;
                return true;
            }
            return false;
        }
    }
    public int getAvailable(Supply.SupplyType type) {
        Supply s = supplies.get(type);
        return s != null ? s.quantity : 0;
    }
    public Map<Supply.SupplyType, Supply> getAvailableSupplies() { return new java.util.HashMap<>(supplies); }
}
