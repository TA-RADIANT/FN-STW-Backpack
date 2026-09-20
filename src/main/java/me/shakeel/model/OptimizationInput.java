package me.shakeel.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the user's initial material inventory and trap preference weights (0-10).
 */
public class OptimizationInput {
    private final Map<Material, Integer> initialMaterials;
    private final Map<TrapRecipe, Double> trapWeights;

    public OptimizationInput(Map<Material, Integer> initialMaterials, Map<TrapRecipe, Double> trapWeights) {
        Map<Material, Integer> matMap = new EnumMap<>(Material.class);
        if (initialMaterials != null) {
            for (Material mat : Material.values()) {
                matMap.put(mat, Math.max(0, initialMaterials.getOrDefault(mat, 0)));
            }
        } else {
            for (Material mat : Material.values()) {
                matMap.put(mat, 0);
            }
        }
        this.initialMaterials = Collections.unmodifiableMap(matMap);

        Map<TrapRecipe, Double> weightMap = new HashMap<>();
        if (trapWeights != null) {
            weightMap.putAll(trapWeights);
        }
        this.trapWeights = Collections.unmodifiableMap(weightMap);
    }

    public Map<Material, Integer> getInitialMaterials() {
        return initialMaterials;
    }

    public int getInitialMaterial(Material material) {
        return initialMaterials.getOrDefault(material, 0);
    }

    public Map<TrapRecipe, Double> getTrapWeights() {
        return trapWeights;
    }

    public double getWeight(TrapRecipe trap) {
        return trapWeights.getOrDefault(trap, trap.getDefaultWeight());
    }

    /**
     * Calculates the total initial slots occupied by all materials.
     */
    public int calculateInitialTotalSlots() {
        int slots = 0;
        for (int qty : initialMaterials.values()) {
            slots += Material.calculateSlots(qty);
        }
        return slots;
    }
}
