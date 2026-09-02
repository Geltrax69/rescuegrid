package com.rescuegrid.model;

public class ResourceRequirement {
    public final ResourceType type;
    public final int count;
    public ResourceRequirement(ResourceType type, int count) { this.type = type; this.count = count; }
}
