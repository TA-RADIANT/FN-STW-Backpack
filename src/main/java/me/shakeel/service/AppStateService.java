package me.shakeel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import me.shakeel.model.AppState;
import me.shakeel.model.Material;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for loading and persisting application state, user materials,
 * trap preference weights, and custom profiles across executions.
 */
public class AppStateService {
    private static final Logger LOGGER = Logger.getLogger(AppStateService.class.getName());
    private static final String DEFAULT_FILE_NAME = "app_state.json";

    private final ObjectMapper objectMapper;
    private final File stateFile;
    private AppState appState;

    public AppStateService() {
        this(new File(DEFAULT_FILE_NAME));
    }

    public AppStateService(File stateFile) {
        this.stateFile = stateFile;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.appState = loadState();
    }

    public AppState getAppState() {
        return appState;
    }

    public synchronized AppState loadState() {
        if (stateFile.exists() && stateFile.isFile()) {
            try {
                AppState loaded = objectMapper.readValue(stateFile, AppState.class);
                if (loaded != null) {
                    LOGGER.info("Loaded saved state from " + stateFile.getAbsolutePath());
                    this.appState = loaded;
                    return loaded;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load state from " + stateFile.getAbsolutePath() + ", initializing defaults", e);
            }
        }
        this.appState = createDefaultState();
        return this.appState;
    }

    public synchronized boolean saveState() {
        try {
            File parent = stateFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            objectMapper.writeValue(stateFile, appState);
            LOGGER.info("Saved application state to " + stateFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save application state to " + stateFile.getAbsolutePath(), e);
            return false;
        }
    }

    private AppState createDefaultState() {
        AppState state = new AppState();

        // Populate default material profiles
        Map<String, Integer> endgame = new LinkedHashMap<>();
        endgame.put(Material.NUTS_AND_BOLTS.getDisplayName(), 8500);
        endgame.put(Material.ROUGH_ORE.getDisplayName(), 6000);
        endgame.put(Material.PLANKS.getDisplayName(), 9500);
        endgame.put(Material.QUARTZ_CRYSTAL.getDisplayName(), 3200);
        endgame.put(Material.FLOWER_PETALS.getDisplayName(), 1500);
        endgame.put(Material.FIBROUS_HERBS.getDisplayName(), 4500);
        endgame.put(Material.DUCT_TAPE.getDisplayName(), 2500);
        endgame.put(Material.BATTERIES.getDisplayName(), 3800);
        endgame.put(Material.BACON.getDisplayName(), 1800);
        endgame.put(Material.MECHANICAL_PARTS.getDisplayName(), 7500);
        endgame.put(Material.TWINE.getDisplayName(), 6200);
        endgame.put(Material.MINERAL_POWDER.getDisplayName(), 4900);
        state.saveMaterialProfile("Endgame Hoarder", endgame);

        Map<String, Integer> balanced = new LinkedHashMap<>();
        for (Material mat : Material.values()) {
            balanced.put(mat.getDisplayName(), 2000);
        }
        state.saveMaterialProfile("Balanced Midgame", balanced);

        Map<String, Integer> woodTwine = new LinkedHashMap<>();
        woodTwine.put(Material.PLANKS.getDisplayName(), 6500);
        woodTwine.put(Material.TWINE.getDisplayName(), 2500);
        woodTwine.put(Material.DUCT_TAPE.getDisplayName(), 1200);
        woodTwine.put(Material.MECHANICAL_PARTS.getDisplayName(), 1800);
        state.saveMaterialProfile("Wood & Twine Focus", woodTwine);

        // Populate default weight profiles
        Map<String, Double> allBalanced = new LinkedHashMap<>();
        allBalanced.put("default", 5.0);
        state.saveWeightProfile("Balanced All (5)", allBalanced);

        Map<String, Double> ceilingHeavy = new LinkedHashMap<>();
        ceilingHeavy.put("ceiling_electric_field", 10.0);
        ceilingHeavy.put("ceiling_gas_trap", 10.0);
        ceilingHeavy.put("ceiling_drop_trap_wall_darts", 8.0);
        ceilingHeavy.put("ceiling_zapper_wall_dynamo", 7.0);
        state.saveWeightProfile("Ceiling Trap Focus", ceilingHeavy);

        Map<String, Double> crowdControl = new LinkedHashMap<>();
        crowdControl.put("tar_pit_sound_wall", 10.0);
        crowdControl.put("wall_lights", 9.0);
        crowdControl.put("floor_launcher_wall_launcher", 9.0);
        crowdControl.put("flame_grill_floor_freeze", 8.0);
        crowdControl.put("wooden_floor_spikes_wall_spikes", 8.0);
        state.saveWeightProfile("Crowd Control & Stalls", crowdControl);

        return state;
    }
}
