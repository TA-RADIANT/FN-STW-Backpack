package me.shakeel.service;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import me.shakeel.model.Material;
import me.shakeel.model.OptimizationInput;
import me.shakeel.model.OptimizationResult;
import me.shakeel.model.TrapRecipe;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service implementing Mixed-Integer Linear Programming (MILP) using Google OR-Tools SCIP solver
 * to calculate optimal trap crafting combinations to minimize total backpack and storage slots.
 */
public class InventoryOptimizerService {
    private static final Logger LOGGER = Logger.getLogger(InventoryOptimizerService.class.getName());
    private static volatile boolean nativeLibrariesLoaded = false;
    private static final Object LOAD_LOCK = new Object();

    public InventoryOptimizerService() {
        ensureNativeLibrariesLoaded();
    }

    private static void ensureNativeLibrariesLoaded() {
        if (!nativeLibrariesLoaded) {
            synchronized (LOAD_LOCK) {
                if (!nativeLibrariesLoaded) {
                    try {
                        Loader.loadNativeLibraries();
                        nativeLibrariesLoaded = true;
                        LOGGER.info("Google OR-Tools native libraries successfully loaded.");
                    } catch (Throwable t) {
                        LOGGER.log(Level.SEVERE, "Failed to load Google OR-Tools native libraries", t);
                        throw new RuntimeException("Could not initialize Google OR-Tools: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * Solves the MILP optimization problem for given inventory and trap recipes.
     *
     * @param input   User initial inventory quantities and trap preference weights
     * @param recipes List of available trap recipes
     * @return OptimizationResult containing before/after slots, traps to craft, and leftovers
     */
    public OptimizationResult optimize(OptimizationInput input, List<TrapRecipe> recipes) {
        long startTime = System.currentTimeMillis();
        int initialTotalSlots = input.calculateInitialTotalSlots();

        // If inventory is completely empty, return zero result immediately
        boolean allZero = true;
        for (Material mat : Material.values()) {
            if (input.getInitialMaterial(mat) > 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            return OptimizationResult.empty(0, "Inventory is empty. No traps can be crafted.");
        }

        MPSolver solver;
        try {
            solver = MPSolver.createSolver("SCIP");
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Error creating SCIP solver", t);
            return OptimizationResult.error(initialTotalSlots, "Error creating SCIP solver: " + t.getMessage());
        }

        if (solver == null) {
            return OptimizationResult.error(initialTotalSlots, "Google OR-Tools SCIP solver is not available.");
        }

        // Variables:
        // x_k: number of trap k to craft (integer >= 0)
        // s_o_k: number of slots for trap k (integer >= 0)
        // s_m_i: number of slots for leftover material i (integer >= 0)
        Map<TrapRecipe, MPVariable> xVars = new HashMap<>();
        Map<TrapRecipe, MPVariable> sTrapVars = new HashMap<>();
        Map<Material, MPVariable> sMatVars = new EnumMap<>(Material.class);

        // Define Trap Variables
        for (TrapRecipe trap : recipes) {
            // Compute safe upper bound for trap k
            int maxCraftable = Integer.MAX_VALUE;
            boolean requiresMat = false;
            for (Map.Entry<Material, Integer> entry : trap.getIngredients().entrySet()) {
                int cost = entry.getValue();
                if (cost > 0) {
                    requiresMat = true;
                    int available = input.getInitialMaterial(entry.getKey());
                    maxCraftable = Math.min(maxCraftable, available / cost);
                }
            }
            if (!requiresMat || maxCraftable == Integer.MAX_VALUE) {
                maxCraftable = 0;
            }

            int maxTrapSlots = TrapRecipe.calculateSlots(maxCraftable);

            MPVariable x = solver.makeIntVar(0.0, maxCraftable, "x_" + trap.getId());
            MPVariable sTrap = solver.makeIntVar(0.0, maxTrapSlots, "s_trap_" + trap.getId());

            xVars.put(trap, x);
            sTrapVars.put(trap, sTrap);
        }

        // Define Material Variables
        for (Material mat : Material.values()) {
            int initialQty = input.getInitialMaterial(mat);
            int maxMatSlots = Material.calculateSlots(initialQty);

            MPVariable sMat = solver.makeIntVar(0.0, maxMatSlots, "s_mat_" + mat.name());
            sMatVars.put(mat, sMat);
        }

        // Constraints:
        // 1. Material Availability: sum(R_ik * x_k) <= Initial_Mat_i
        for (Material mat : Material.values()) {
            int initialQty = input.getInitialMaterial(mat);
            MPConstraint matAvailConstraint = solver.makeConstraint(
                    -MPSolver.infinity(),
                    (double) initialQty,
                    "avail_" + mat.name()
            );
            for (TrapRecipe trap : recipes) {
                int cost = trap.getCost(mat);
                if (cost > 0) {
                    matAvailConstraint.setCoefficient(xVars.get(trap), cost);
                }
            }
        }

        // 2. Trap Slot Capacity: x_k <= 200 * s_o_k  ==>  x_k - 200 * s_o_k <= 0
        for (TrapRecipe trap : recipes) {
            MPConstraint trapSlotConstraint = solver.makeConstraint(
                    -MPSolver.infinity(),
                    0.0,
                    "trap_slot_cap_" + trap.getId()
            );
            trapSlotConstraint.setCoefficient(xVars.get(trap), 1.0);
            trapSlotConstraint.setCoefficient(sTrapVars.get(trap), -200.0);
        }

        // 3. Leftover Material Slot Capacity:
        // Initial_Mat_i - sum(R_ik * x_k) <= 999 * s_m_i
        // ==> sum(R_ik * x_k) + 999 * s_m_i >= Initial_Mat_i
        for (Material mat : Material.values()) {
            int initialQty = input.getInitialMaterial(mat);
            MPConstraint leftoverConstraint = solver.makeConstraint(
                    (double) initialQty,
                    MPSolver.infinity(),
                    "leftover_cap_" + mat.name()
            );
            for (TrapRecipe trap : recipes) {
                int cost = trap.getCost(mat);
                if (cost > 0) {
                    leftoverConstraint.setCoefficient(xVars.get(trap), cost);
                }
            }
            leftoverConstraint.setCoefficient(sMatVars.get(mat), 999.0);
        }

        // Objective Function:
        // Minimize: sum(s_o_k) + sum(s_m_i) - 0.0001 * sum(w_k * x_k)
        MPObjective objective = solver.objective();
        objective.setMinimization();

        for (TrapRecipe trap : recipes) {
            objective.setCoefficient(sTrapVars.get(trap), 1.0);
            double weight = input.getWeight(trap);
            // Tie breaker weight incentive: -0.0001 * w_k
            objective.setCoefficient(xVars.get(trap), -0.0001 * weight);
        }

        for (Material mat : Material.values()) {
            objective.setCoefficient(sMatVars.get(mat), 1.0);
        }

        // Solve
        MPSolver.ResultStatus resultStatus = solver.solve();
        long executionTime = System.currentTimeMillis() - startTime;

        if (resultStatus != MPSolver.ResultStatus.OPTIMAL && resultStatus != MPSolver.ResultStatus.FEASIBLE) {
            return new OptimizationResult(
                    OptimizationResult.Status.INFEASIBLE,
                    initialTotalSlots,
                    initialTotalSlots,
                    Map.of(),
                    Map.of(),
                    input.getInitialMaterials(),
                    calculateInitialMaterialSlots(input),
                    Map.of(),
                    executionTime,
                    "Solver finished with status: " + resultStatus
            );
        }

        // Extract solutions
        Map<TrapRecipe, Integer> trapsToCraft = new LinkedHashMap<>();
        Map<TrapRecipe, Integer> trapSlots = new LinkedHashMap<>();
        for (TrapRecipe trap : recipes) {
            int count = (int) Math.round(xVars.get(trap).solutionValue());
            if (count > 0) {
                trapsToCraft.put(trap, count);
                trapSlots.put(trap, TrapRecipe.calculateSlots(count));
            }
        }

        // Calculate consumed, leftover materials and slots
        Map<Material, Integer> consumed = new EnumMap<>(Material.class);
        Map<Material, Integer> leftovers = new EnumMap<>(Material.class);
        Map<Material, Integer> leftoverSlots = new EnumMap<>(Material.class);

        int totalSlotsAfter = 0;
        for (int slots : trapSlots.values()) {
            totalSlotsAfter += slots;
        }

        for (Material mat : Material.values()) {
            int initialQty = input.getInitialMaterial(mat);
            int consumedQty = 0;
            for (Map.Entry<TrapRecipe, Integer> entry : trapsToCraft.entrySet()) {
                consumedQty += entry.getKey().getCost(mat) * entry.getValue();
            }
            int remaining = Math.max(0, initialQty - consumedQty);
            int remainingSlots = Material.calculateSlots(remaining);

            consumed.put(mat, consumedQty);
            leftovers.put(mat, remaining);
            leftoverSlots.put(mat, remainingSlots);

            totalSlotsAfter += remainingSlots;
        }

        OptimizationResult.Status status = (resultStatus == MPSolver.ResultStatus.OPTIMAL)
                ? OptimizationResult.Status.OPTIMAL
                : OptimizationResult.Status.FEASIBLE;

        return new OptimizationResult(
                status,
                initialTotalSlots,
                totalSlotsAfter,
                trapsToCraft,
                trapSlots,
                leftovers,
                leftoverSlots,
                consumed,
                executionTime,
                "Optimization completed successfully in " + executionTime + " ms."
        );
    }

    private Map<Material, Integer> calculateInitialMaterialSlots(OptimizationInput input) {
        Map<Material, Integer> slots = new EnumMap<>(Material.class);
        for (Material mat : Material.values()) {
            slots.put(mat, Material.calculateSlots(input.getInitialMaterial(mat)));
        }
        return slots;
    }
}
