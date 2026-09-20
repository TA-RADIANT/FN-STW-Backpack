package me.shakeel.model;

import java.util.Arrays;

/**
 * The 12 Raw Crafting Materials in Fortnite: Save the World.
 * Max stack size in backpack/storage is 999 per inventory slot.
 */
public enum Material {
    MECHANICAL_PARTS("Mechanical Parts"),
    TWINE("Twine"),
    QUARTZ_CRYSTAL("Quartz Crystal"),
    DUCT_TAPE("Duct Tape"),
    MINERAL_POWDER("Mineral Powder"),
    BACON("Bacon"),
    BATTERIES("Batteries"),
    FIBROUS_HERBS("Fibrous Herbs"),
    FLOWER_PETALS("Flower Petals"),
    NUTS_AND_BOLTS("Nuts 'n' Bolts"),
    PLANKS("Planks"),
    ROUGH_ORE("Rough Ore");

    public static final int MAX_STACK_SIZE = 999;

    private final String displayName;

    Material(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxStackSize() {
        return MAX_STACK_SIZE;
    }

    /**
     * Calculates the number of inventory slots required for a given material quantity.
     * 0 items = 0 slots; 1..999 = 1 slot; 1000..1998 = 2 slots, etc.
     */
    public static int calculateSlots(int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        return (quantity + MAX_STACK_SIZE - 1) / MAX_STACK_SIZE;
    }

    public static Material fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (Material mat : values()) {
            if (mat.displayName.equalsIgnoreCase(displayName.trim()) ||
                mat.name().equalsIgnoreCase(displayName.trim())) {
                return mat;
            }
        }
        throw new IllegalArgumentException("Unknown material: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
