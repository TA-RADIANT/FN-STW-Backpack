package me.shakeel.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates the results of the inventory trap-crafting optimization.
 */
public class OptimizationResult {
    public enum Status {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        UNBOUNDED,
        ERROR
    }

    private final Status status;
    private final int totalSlotsBefore;
    private final int totalSlotsAfter;
    private final double slotReductionPercent;
    private final Map<TrapRecipe, Integer> trapsToCraft;
    private final Map<TrapRecipe, Integer> trapSlots;
    private final Map<Material, Integer> leftoverMaterials;
    private final Map<Material, Integer> leftoverSlots;
    private final Map<Material, Integer> consumedMaterials;
    private final long executionTimeMs;
    private final String message;

    public OptimizationResult(
            Status status,
            int totalSlotsBefore,
            int totalSlotsAfter,
            Map<TrapRecipe, Integer> trapsToCraft,
            Map<TrapRecipe, Integer> trapSlots,
            Map<Material, Integer> leftoverMaterials,
            Map<Material, Integer> leftoverSlots,
            Map<Material, Integer> consumedMaterials,
            long executionTimeMs,
            String message) {
        this.status = status;
        this.totalSlotsBefore = totalSlotsBefore;
        this.totalSlotsAfter = totalSlotsAfter;
        this.slotReductionPercent = totalSlotsBefore > 0
                ? Math.max(0.0, ((double) (totalSlotsBefore - totalSlotsAfter) / totalSlotsBefore) * 100.0)
                : 0.0;
        this.trapsToCraft = trapsToCraft != null ? Collections.unmodifiableMap(new LinkedHashMap<>(trapsToCraft)) : Collections.emptyMap();
        this.trapSlots = trapSlots != null ? Collections.unmodifiableMap(new LinkedHashMap<>(trapSlots)) : Collections.emptyMap();
        Map<Material, Integer> lMat = new EnumMap<>(Material.class);
        if (leftoverMaterials != null) lMat.putAll(leftoverMaterials);
        this.leftoverMaterials = Collections.unmodifiableMap(lMat);

        Map<Material, Integer> lSlots = new EnumMap<>(Material.class);
        if (leftoverSlots != null) lSlots.putAll(leftoverSlots);
        this.leftoverSlots = Collections.unmodifiableMap(lSlots);

        Map<Material, Integer> cMat = new EnumMap<>(Material.class);
        if (consumedMaterials != null) cMat.putAll(consumedMaterials);
        this.consumedMaterials = Collections.unmodifiableMap(cMat);
        this.executionTimeMs = executionTimeMs;
        this.message = message != null ? message : "";
    }

    public static OptimizationResult empty(int totalSlotsBefore, String message) {
        return new OptimizationResult(
                Status.OPTIMAL,
                totalSlotsBefore,
                totalSlotsBefore,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                0,
                message
        );
    }

    public static OptimizationResult error(int totalSlotsBefore, String errorMessage) {
        return new OptimizationResult(
                Status.ERROR,
                totalSlotsBefore,
                totalSlotsBefore,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                0,
                errorMessage
        );
    }

    public Status getStatus() {
        return status;
    }

    public int getTotalSlotsBefore() {
        return totalSlotsBefore;
    }

    public int getTotalSlotsAfter() {
        return totalSlotsAfter;
    }

    public int getSlotsSaved() {
        return Math.max(0, totalSlotsBefore - totalSlotsAfter);
    }

    public double getSlotReductionPercent() {
        return slotReductionPercent;
    }

    public Map<TrapRecipe, Integer> getTrapsToCraft() {
        return trapsToCraft;
    }

    public Map<TrapRecipe, Integer> getTrapSlots() {
        return trapSlots;
    }

    public Map<Material, Integer> getLeftoverMaterials() {
        return leftoverMaterials;
    }

    public Map<Material, Integer> getLeftoverSlots() {
        return leftoverSlots;
    }

    public Map<Material, Integer> getConsumedMaterials() {
        return consumedMaterials;
    }

    public int getTotalTrapsCrafted() {
        return trapsToCraft.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalTrapSlots() {
        return trapSlots.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalLeftoverSlots() {
        return leftoverSlots.values().stream().mapToInt(Integer::intValue).sum();
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getMessage() {
        return message;
    }
}
