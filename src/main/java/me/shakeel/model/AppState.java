package me.shakeel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * State container for persisting user raw materials, trap preference weights,
 * and custom named profiles across application executions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppState {

    private Map<String, Integer> currentMaterials = new LinkedHashMap<>();
    private Map<String, Double> currentTrapWeights = new LinkedHashMap<>();

    private Map<String, Map<String, Integer>> materialProfiles = new LinkedHashMap<>();
    private Map<String, Map<String, Double>> weightProfiles = new LinkedHashMap<>();

    public AppState() {}

    public Map<String, Integer> getCurrentMaterials() {
        return currentMaterials;
    }

    public void setCurrentMaterials(Map<String, Integer> currentMaterials) {
        this.currentMaterials = currentMaterials != null ? new LinkedHashMap<>(currentMaterials) : new LinkedHashMap<>();
    }

    public Map<String, Double> getCurrentTrapWeights() {
        return currentTrapWeights;
    }

    public void setCurrentTrapWeights(Map<String, Double> currentTrapWeights) {
        this.currentTrapWeights = currentTrapWeights != null ? new LinkedHashMap<>(currentTrapWeights) : new LinkedHashMap<>();
    }

    public Map<String, Map<String, Integer>> getMaterialProfiles() {
        return materialProfiles;
    }

    public void setMaterialProfiles(Map<String, Map<String, Integer>> materialProfiles) {
        this.materialProfiles = materialProfiles != null ? new LinkedHashMap<>(materialProfiles) : new LinkedHashMap<>();
    }

    public Map<String, Map<String, Double>> getWeightProfiles() {
        return weightProfiles;
    }

    public void setWeightProfiles(Map<String, Map<String, Double>> weightProfiles) {
        this.weightProfiles = weightProfiles != null ? new LinkedHashMap<>(weightProfiles) : new LinkedHashMap<>();
    }

    public void saveMaterialProfile(String name, Map<String, Integer> materials) {
        if (name != null && !name.trim().isEmpty() && materials != null) {
            materialProfiles.put(name.trim(), new LinkedHashMap<>(materials));
        }
    }

    public void removeMaterialProfile(String name) {
        if (name != null) {
            materialProfiles.remove(name.trim());
        }
    }

    public void saveWeightProfile(String name, Map<String, Double> weights) {
        if (name != null && !name.trim().isEmpty() && weights != null) {
            weightProfiles.put(name.trim(), new LinkedHashMap<>(weights));
        }
    }

    public void removeWeightProfile(String name) {
        if (name != null) {
            weightProfiles.remove(name.trim());
        }
    }
}
