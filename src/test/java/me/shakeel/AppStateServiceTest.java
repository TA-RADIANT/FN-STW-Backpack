package me.shakeel;

import me.shakeel.model.AppState;
import me.shakeel.model.Material;
import me.shakeel.service.AppStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AppStateServiceTest {

    @Test
    @DisplayName("Verify AppState saving, custom profile persistence, and restoration")
    void testStatePersistence(@TempDir Path tempDir) {
        File stateFile = tempDir.resolve("test_state.json").toFile();
        AppStateService service = new AppStateService(stateFile);

        AppState state = service.getAppState();
        assertNotNull(state);

        // Add custom materials and weights
        state.setCurrentMaterials(Map.of(
                Material.NUTS_AND_BOLTS.getDisplayName(), 3500,
                Material.BATTERIES.getDisplayName(), 1200
        ));
        state.setCurrentTrapWeights(Map.of(
                "ceiling_electric_field", 9.0,
                "anti_air_trap", 8.0
        ));

        // Save custom profiles
        state.saveMaterialProfile("My Farming Run", Map.of("Planks", 5000));
        state.saveWeightProfile("Anti-Air Heavy", Map.of("anti_air_trap", 10.0));

        boolean saved = service.saveState();
        assertTrue(saved);
        assertTrue(stateFile.exists());

        // Reload in a fresh service instance
        AppStateService reloadedService = new AppStateService(stateFile);
        AppState reloadedState = reloadedService.getAppState();

        assertNotNull(reloadedState);
        assertEquals(3500, reloadedState.getCurrentMaterials().get(Material.NUTS_AND_BOLTS.getDisplayName()));
        assertEquals(9.0, reloadedState.getCurrentTrapWeights().get("ceiling_electric_field"));
        assertTrue(reloadedState.getMaterialProfiles().containsKey("My Farming Run"));
        assertTrue(reloadedState.getWeightProfiles().containsKey("Anti-Air Heavy"));
    }
}
