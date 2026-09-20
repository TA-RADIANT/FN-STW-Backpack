package me.shakeel;

import me.shakeel.model.Material;
import me.shakeel.model.OptimizationInput;
import me.shakeel.model.OptimizationResult;
import me.shakeel.model.TrapRecipe;
import me.shakeel.service.InventoryOptimizerService;
import me.shakeel.service.RecipeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryOptimizerServiceTest {

    private static InventoryOptimizerService service;
    private static RecipeRepository repository;

    @BeforeAll
    static void setUp() {
        service = new InventoryOptimizerService();
        repository = new RecipeRepository();
    }

    @Test
    @DisplayName("Verify slot ceiling calculations for materials (999/slot) and traps (200/slot)")
    void testSlotCeilingCalculations() {
        // Materials: max stack 999
        assertEquals(0, Material.calculateSlots(-5));
        assertEquals(0, Material.calculateSlots(0));
        assertEquals(1, Material.calculateSlots(1));
        assertEquals(1, Material.calculateSlots(500));
        assertEquals(1, Material.calculateSlots(999));
        assertEquals(2, Material.calculateSlots(1000));
        assertEquals(2, Material.calculateSlots(1998));
        assertEquals(3, Material.calculateSlots(1999));

        // Traps: max stack 200
        assertEquals(0, TrapRecipe.calculateSlots(-10));
        assertEquals(0, TrapRecipe.calculateSlots(0));
        assertEquals(1, TrapRecipe.calculateSlots(1));
        assertEquals(1, TrapRecipe.calculateSlots(100));
        assertEquals(1, TrapRecipe.calculateSlots(200));
        assertEquals(2, TrapRecipe.calculateSlots(201));
        assertEquals(2, TrapRecipe.calculateSlots(400));
        assertEquals(3, TrapRecipe.calculateSlots(401));
    }

    @Test
    @DisplayName("Verify conservation of materials: Consumed + Leftover == Initial for all materials")
    void testMaterialConservation() {
        Map<Material, Integer> initial = new EnumMap<>(Material.class);
        initial.put(Material.NUTS_AND_BOLTS, 5000);
        initial.put(Material.ROUGH_ORE, 3500);
        initial.put(Material.PLANKS, 8000);
        initial.put(Material.QUARTZ_CRYSTAL, 1200);
        initial.put(Material.FLOWER_PETALS, 600);
        initial.put(Material.FIBROUS_HERBS, 4000);
        initial.put(Material.DUCT_TAPE, 1500);
        initial.put(Material.BATTERIES, 2200);
        initial.put(Material.BACON, 1000);
        initial.put(Material.MECHANICAL_PARTS, 6000);
        initial.put(Material.TWINE, 4500);
        initial.put(Material.MINERAL_POWDER, 3200);

        OptimizationInput input = new OptimizationInput(initial, Map.of());
        OptimizationResult result = service.optimize(input, repository.getRecipes());

        assertNotNull(result);
        assertTrue(result.getStatus() == OptimizationResult.Status.OPTIMAL ||
                   result.getStatus() == OptimizationResult.Status.FEASIBLE);

        for (Material mat : Material.values()) {
            int init = initial.get(mat);
            int consumed = result.getConsumedMaterials().getOrDefault(mat, 0);
            int leftover = result.getLeftoverMaterials().getOrDefault(mat, 0);

            assertTrue(consumed >= 0, "Consumed must be non-negative for " + mat);
            assertTrue(leftover >= 0, "Leftover must be non-negative for " + mat);
            assertEquals(init, consumed + leftover, "Conservation violated for " + mat);
        }
    }

    @Test
    @DisplayName("Verify tie-breaking behavior: Higher-weighted trap is crafted when costs are identical")
    void testTieBreakingBehavior() {
        // Create two competing traps with IDENTICAL material costs
        TrapRecipe trapAlpha = new TrapRecipe(
                "trap_alpha",
                "Trap Alpha",
                "Ceiling",
                Map.of(Material.PLANKS, 10, Material.TWINE, 5),
                5.0
        );

        TrapRecipe trapBeta = new TrapRecipe(
                "trap_beta",
                "Trap Beta",
                "Floor",
                Map.of(Material.PLANKS, 10, Material.TWINE, 5),
                5.0
        );

        List<TrapRecipe> competingRecipes = List.of(trapAlpha, trapBeta);

        Map<Material, Integer> initial = new EnumMap<>(Material.class);
        initial.put(Material.PLANKS, 2000);
        initial.put(Material.TWINE, 1000);

        // Case 1: Alpha has higher weight (9.0) than Beta (2.0)
        Map<TrapRecipe, Double> weights1 = new HashMap<>();
        weights1.put(trapAlpha, 9.0);
        weights1.put(trapBeta, 2.0);

        OptimizationInput input1 = new OptimizationInput(initial, weights1);
        OptimizationResult result1 = service.optimize(input1, competingRecipes);

        int alphaCrafted1 = result1.getTrapsToCraft().getOrDefault(trapAlpha, 0);
        int betaCrafted1 = result1.getTrapsToCraft().getOrDefault(trapBeta, 0);

        assertTrue(alphaCrafted1 > 0, "Alpha (higher weight) should be crafted");
        assertEquals(0, betaCrafted1, "Beta (lower weight) should not be crafted when Alpha is preferred");

        // Case 2: Beta has higher weight (8.0) than Alpha (1.0)
        Map<TrapRecipe, Double> weights2 = new HashMap<>();
        weights2.put(trapAlpha, 1.0);
        weights2.put(trapBeta, 8.0);

        OptimizationInput input2 = new OptimizationInput(initial, weights2);
        OptimizationResult result2 = service.optimize(input2, competingRecipes);

        int alphaCrafted2 = result2.getTrapsToCraft().getOrDefault(trapAlpha, 0);
        int betaCrafted2 = result2.getTrapsToCraft().getOrDefault(trapBeta, 0);

        assertTrue(betaCrafted2 > 0, "Beta (higher weight) should be crafted");
        assertEquals(0, alphaCrafted2, "Alpha (lower weight) should not be crafted when Beta is preferred");
    }

    @Test
    @DisplayName("Verify empty inventory handling")
    void testEmptyInventory() {
        Map<Material, Integer> empty = new EnumMap<>(Material.class);
        for (Material mat : Material.values()) {
            empty.put(mat, 0);
        }

        OptimizationInput input = new OptimizationInput(empty, Map.of());
        OptimizationResult result = service.optimize(input, repository.getRecipes());

        assertNotNull(result);
        assertEquals(0, result.getTotalSlotsBefore());
        assertEquals(0, result.getTotalSlotsAfter());
        assertEquals(0, result.getTotalTrapsCrafted());
    }

    @Test
    @DisplayName("Verify decoupled recipe loading loads all 18 traps")
    void testRecipeLoading() {
        List<TrapRecipe> recipes = repository.getRecipes();
        assertNotNull(recipes);
        assertEquals(18, recipes.size(), "Should contain exactly 18 traps/trap groups");

        for (TrapRecipe recipe : recipes) {
            assertNotNull(recipe.getId());
            assertNotNull(recipe.getName());
            assertFalse(recipe.getIngredients().isEmpty(), "Trap " + recipe.getName() + " must have ingredients");
        }
    }

    @Test
    @DisplayName("Verify storage slot optimization compresses multi-stack materials")
    void testSlotReduction() {
        // Provide large inventory where turning materials into traps significantly compresses slot count
        // Cozy Campfire from trapRecipes.csv: 9 Planks, 2 Flower Petals, 2 Twine
        Map<Material, Integer> initial = new EnumMap<>(Material.class);
        initial.put(Material.PLANKS, 3600);        // 4 slots
        initial.put(Material.FLOWER_PETALS, 800);  // 1 slot
        initial.put(Material.TWINE, 800);          // 1 slot
        // Total before: 6 slots
        // 400 Cozy Campfires = 2 slots of traps
        // 400 * 9 = 3600 Planks (0 leftover = 0 slots)
        // 400 * 2 = 800 Flower Petals (0 leftover = 0 slots)
        // 400 * 2 = 800 Twine (0 leftover = 0 slots)
        // Total after: 2 slots! Reduction: 6 -> 2 (66.7% reduction)

        OptimizationInput input = new OptimizationInput(initial, Map.of());
        OptimizationResult result = service.optimize(input, repository.getRecipes());

        assertEquals(6, result.getTotalSlotsBefore());
        assertTrue(result.getTotalSlotsAfter() < result.getTotalSlotsBefore());
        assertTrue(result.getSlotReductionPercent() > 50.0);
    }
}
