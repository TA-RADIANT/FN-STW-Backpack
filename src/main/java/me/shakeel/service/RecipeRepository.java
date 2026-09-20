package me.shakeel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.shakeel.model.Material;
import me.shakeel.model.TrapRecipe;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for managing trap recipes, decoupled into external/embedded recipes.json.
 */
public class RecipeRepository {
    private static final Logger LOGGER = Logger.getLogger(RecipeRepository.class.getName());
    private static final String DEFAULT_RECIPES_RESOURCE = "/recipes.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TrapRecipe> recipes;

    public RecipeRepository() {
        this.recipes = loadDefaultRecipes();
    }

    public List<TrapRecipe> getRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public void setRecipes(List<TrapRecipe> recipes) {
        if (recipes != null && !recipes.isEmpty()) {
            this.recipes = new ArrayList<>(recipes);
        }
    }

    public List<TrapRecipe> loadDefaultRecipes() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_RECIPES_RESOURCE)) {
            if (is != null) {
                List<TrapRecipe> loaded = objectMapper.readValue(is, new TypeReference<List<TrapRecipe>>() {});
                if (loaded != null && !loaded.isEmpty()) {
                    return loaded;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load embedded recipes.json, falling back to programmatic defaults", e);
        }
        return createFallbackRecipes();
    }

    public List<TrapRecipe> loadFromFile(File file) throws Exception {
        List<TrapRecipe> loaded = objectMapper.readValue(file, new TypeReference<List<TrapRecipe>>() {});
        if (loaded != null && !loaded.isEmpty()) {
            this.recipes = new ArrayList<>(loaded);
            return this.recipes;
        }
        throw new IllegalArgumentException("No recipes found in file: " + file.getName());
    }

    public void saveToFile(File file) throws Exception {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, recipes);
    }

    private List<TrapRecipe> createFallbackRecipes() {
        List<TrapRecipe> list = new ArrayList<>();

        list.add(createRecipe("anti_air_trap", "Anti-Air Trap", "Ceiling / Floor", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 6, Material.PLANKS, 3, Material.QUARTZ_CRYSTAL, 1)));

        list.add(createRecipe("broadside", "Broadside", "Wall", 5.0,
                Map.of(Material.PLANKS, 7, Material.MECHANICAL_PARTS, 2, Material.MINERAL_POWDER, 2)));

        list.add(createRecipe("ceiling_drop_trap_wall_darts", "Ceiling Drop Trap / Wall Darts", "Ceiling / Wall", 5.0,
                Map.of(Material.PLANKS, 7, Material.MECHANICAL_PARTS, 2, Material.TWINE, 2)));

        list.add(createRecipe("ceiling_electric_field", "Ceiling Electric Field", "Ceiling", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 8, Material.ROUGH_ORE, 3, Material.BATTERIES, 2, Material.MINERAL_POWDER, 2)));

        list.add(createRecipe("ceiling_gas_trap", "Ceiling Gas Trap", "Ceiling", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 7, Material.FIBROUS_HERBS, 5, Material.BACON, 1, Material.MINERAL_POWDER, 2)));

        list.add(createRecipe("ceiling_zapper", "Ceiling Zapper", "Ceiling", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 9, Material.BATTERIES, 1, Material.MECHANICAL_PARTS, 3)));

        list.add(createRecipe("cozy_campfire", "Cozy Campfire", "Floor", 5.0,
                Map.of(Material.PLANKS, 9, Material.FLOWER_PETALS, 2, Material.TWINE, 2)));

        list.add(createRecipe("flame_grill_floor_freeze", "Floor Freeze Trap / Flame Grill Floor Trap", "Floor", 5.0,
                Map.of(Material.ROUGH_ORE, 7, Material.QUARTZ_CRYSTAL, 2, Material.MINERAL_POWDER, 2)));

        list.add(createRecipe("floor_launcher_wall_launcher", "Floor Launcher / Wall Launcher", "Floor / Wall", 5.0,
                Map.of(Material.ROUGH_ORE, 4, Material.PLANKS, 6, Material.MINERAL_POWDER, 2)));

        list.add(createRecipe("healing_pad", "Healing Pad", "Floor", 5.0,
                Map.of(Material.FLOWER_PETALS, 4, Material.FIBROUS_HERBS, 2, Material.BACON, 1)));

        list.add(createRecipe("jump_boost_pad", "Jump Boost Pad", "Floor", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 2, Material.PLANKS, 1)));

        list.add(createRecipe("retractable_floor_spikes", "Retractable Floor Spikes", "Floor", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 9, Material.ROUGH_ORE, 2, Material.MECHANICAL_PARTS, 2)));

        list.add(createRecipe("sound_wall", "Sound Wall", "Wall", 5.0,
                Map.of(Material.ROUGH_ORE, 7, Material.QUARTZ_CRYSTAL, 1, Material.MECHANICAL_PARTS, 3)));

        list.add(createRecipe("tar_pit", "Tar Pit", "Floor", 5.0,
                Map.of(Material.ROUGH_ORE, 7, Material.QUARTZ_CRYSTAL, 2, Material.MECHANICAL_PARTS, 2)));

        list.add(createRecipe("wall_dynamo", "Wall Dynamo", "Wall", 5.0,
                Map.of(Material.NUTS_AND_BOLTS, 8, Material.BATTERIES, 1, Material.MECHANICAL_PARTS, 3)));

        list.add(createRecipe("wall_lights", "Wall Lights", "Wall", 5.0,
                Map.of(Material.PLANKS, 5, Material.QUARTZ_CRYSTAL, 2, Material.MECHANICAL_PARTS, 2)));

        list.add(createRecipe("wall_spikes", "Wall Spikes", "Wall", 5.0,
                Map.of(Material.PLANKS, 3, Material.DUCT_TAPE, 1, Material.TWINE, 2)));

        list.add(createRecipe("wooden_floor_spikes", "Wooden Floor Spikes", "Floor", 5.0,
                Map.of(Material.PLANKS, 5, Material.DUCT_TAPE, 1, Material.TWINE, 2)));

        return list;
    }

    private TrapRecipe createRecipe(String id, String name, String category, double defaultWeight, Map<Material, Integer> ingredients) {
        return new TrapRecipe(id, name, category, new EnumMap<>(ingredients), defaultWeight);
    }
}
