package com.rescuegrid.model;

public class Supply {
    public final SupplyType type;
    public int quantity;

    public Supply(SupplyType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public enum SupplyType { MEDICAL_KITS, BLOOD_UNITS, OXYGEN, FOOD, WATER }
}
