package me.shakeel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a craftable trap or trap group in Fortnite: Save the World.
 * Max stack size in backpack/storage is 200 per inventory slot.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrapRecipe {
    public static final int MAX_STACK_SIZE = 200;

    private final String id;
    private final String name;
    private final String category;
    private final Map<Material, Integer> ingredients;
    private final double defaultWeight;

    @JsonCreator
    public TrapRecipe(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("category") String category,
            @JsonProperty("ingredients") Map<String, Integer> rawIngredients,
            @JsonProperty(value = "defaultWeight", defaultValue = "5.0") Double defaultWeight) {
        this.id = id != null ? id : (name != null ? name.toLowerCase().replace(' ', '_') : "unknown");
        this.name = name != null ? name : "Unknown Trap";
        this.category = category != null ? category : "General";
        this.defaultWeight = defaultWeight != null ? defaultWeight : 5.0;

        Map<Material, Integer> map = new EnumMap<>(Material.class);
        if (rawIngredients != null) {
            for (Map.Entry<String, Integer> entry : rawIngredients.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    Material mat = Material.fromDisplayName(entry.getKey());
                    map.put(mat, entry.getValue());
                }
            }
        }
        this.ingredients = Collections.unmodifiableMap(map);
    }

    public TrapRecipe(String id, String name, String category, Map<Material, Integer> ingredients, double defaultWeight) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defaultWeight = defaultWeight;
        this.ingredients = Collections.unmodifiableMap(new EnumMap<>(ingredients));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Map<Material, Integer> getIngredients() {
        return ingredients;
    }

    public int getCost(Material material) {
        return ingredients.getOrDefault(material, 0);
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }

    public int getMaxStackSize() {
        return MAX_STACK_SIZE;
    }

    /**
     * Calculates the number of inventory slots required for a given trap quantity.
     * 0 items = 0 slots; 1..200 = 1 slot; 201..400 = 2 slots, etc.
     */
    public static int calculateSlots(int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        return (quantity + MAX_STACK_SIZE - 1) / MAX_STACK_SIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrapRecipe that = (TrapRecipe) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
