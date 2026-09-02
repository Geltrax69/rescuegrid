package com.rescuegrid.model;

public enum TrafficLevel {
    LOW(0.0), MEDIUM(0.15), HIGH(0.35), VERY_HIGH(0.6);
    private final double speedReduction;
    TrafficLevel(double speedReduction) { this.speedReduction = speedReduction; }
    public double getSpeedReduction() { return speedReduction; }
}
