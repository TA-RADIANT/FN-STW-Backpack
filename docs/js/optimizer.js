/**
 * Backpack & Storage MILP Optimization Engine.
 * Translates user inventory and trap weights into a Mixed-Integer Linear Program (MILP),
 * solved client-side by javascript-lp-solver.
 */

class InventoryOptimizer {
    constructor(materials, recipes) {
        this.materials = materials;
        this.recipes = recipes;
    }

    /**
     * Run the optimization.
     * @param {Object} inputMaterials Map of material name -> quantity (number)
     * @param {Object} inputWeights Map of trap id -> weight (0 to 10)
     * @returns {Object} Optimization results
     */
    optimize(inputMaterials, inputWeights) {
        const startTime = performance.now();

        // Calculate initial slots
        let beforeSlots = 0;
        let totalItems = 0;
        const initialMap = {};

        for (const mat of this.materials) {
            const qty = Math.max(0, parseInt(inputMaterials[mat.name], 10) || 0);
            initialMap[mat.name] = qty;
            totalItems += qty;
            beforeSlots += calculateMatSlots(qty);
        }

        if (totalItems === 0) {
            return {
                status: "EMPTY",
                beforeSlots: 0,
                afterSlots: 0,
                slotsSaved: 0,
                reductionPercent: 0,
                trapsToCraft: [],
                leftovers: this.materials.map(m => ({
                    name: m.name,
                    remaining: 0,
                    slots: 0,
                    consumed: 0
                })),
                totalTrapsCrafted: 0,
                executionTimeMs: 0,
                message: "Inventory is empty. Enter material counts to optimize."
            };
        }

        // Try MILP Solver via javascript-lp-solver
        let result = null;
        if (typeof solver !== "undefined" && solver.Solve) {
            try {
                result = this.solveWithMILP(initialMap, inputWeights, beforeSlots);
            } catch (err) {
                console.warn("MILP Solver encountered an issue, falling back to heuristic optimizer:", err);
            }
        }

        // If MILP solver was not feasible or threw an error, fall back to heuristic
        if (!result || result.status !== "SUCCESS") {
            result = this.solveWithHeuristic(initialMap, inputWeights, beforeSlots);
        }

        result.executionTimeMs = Math.round(performance.now() - startTime);
        return result;
    }

    solveWithMILP(initialMap, inputWeights, beforeSlots) {
        const constraints = {};
        const variables = {};
        const ints = {};

        // 1. Constraints for each material
        for (const mat of this.materials) {
            const initialQty = initialMap[mat.name] || 0;
            // Availability: sum(cost * x) <= initialQty
            constraints[`avail_${mat.id}`] = { max: initialQty };
            // Leftover slots: sum(cost * x) + 999 * s_mat >= initialQty
            constraints[`leftover_cap_${mat.id}`] = { min: initialQty };

            // Variable for leftover material slots: s_mat
            const sMatVarName = `s_mat_${mat.id}`;
            variables[sMatVarName] = {
                [`leftover_cap_${mat.id}`]: 999,
                objective: 1.0
            };
            ints[sMatVarName] = 1;
        }

        // 2. Constraints and variables for each trap
        for (const trap of this.recipes) {
            const trapSlotCapName = `trap_slot_cap_${trap.id}`;
            // Trap slots: x - 200 * s_trap <= 0
            constraints[trapSlotCapName] = { max: 0 };

            const sTrapVarName = `s_trap_${trap.id}`;
            variables[sTrapVarName] = {
                [trapSlotCapName]: -200,
                objective: 1.0
            };
            ints[sTrapVarName] = 1;

            const xVarName = `x_${trap.id}`;
            const xVarObj = {
                [trapSlotCapName]: 1,
                // Objective weight incentive: -0.0001 * weight
                objective: -0.0001 * (inputWeights[trap.id] !== undefined ? inputWeights[trap.id] : trap.defaultWeight)
            };

            for (const [matName, cost] of Object.entries(trap.ingredients)) {
                const matDef = this.materials.find(m => m.name === matName);
                if (matDef && cost > 0) {
                    xVarObj[`avail_${matDef.id}`] = cost;
                    xVarObj[`leftover_cap_${matDef.id}`] = cost;
                }
            }

            variables[xVarName] = xVarObj;
            ints[xVarName] = 1;
        }

        const model = {
            optimize: "objective",
            opType: "min",
            constraints: constraints,
            variables: variables,
            ints: ints
        };

        const solution = solver.Solve(model);
        if (!solution || !solution.feasible) {
            return { status: "INFEASIBLE" };
        }

        // Parse solution
        const trapsToCraft = [];
        let totalTrapsCrafted = 0;
        let totalTrapSlots = 0;
        const consumed = {};
        for (const mat of this.materials) consumed[mat.name] = 0;

        for (const trap of this.recipes) {
            const xVarName = `x_${trap.id}`;
            const rawVal = solution[xVarName];
            const qty = rawVal ? Math.round(rawVal) : 0;

            if (qty > 0) {
                const slots = calculateTrapSlots(qty);
                totalTrapsCrafted += qty;
                totalTrapSlots += slots;
                trapsToCraft.push({
                    id: trap.id,
                    name: trap.name,
                    category: trap.category,
                    quantity: qty,
                    slots: slots
                });

                for (const [matName, cost] of Object.entries(trap.ingredients)) {
                    consumed[matName] = (consumed[matName] || 0) + (cost * qty);
                }
            }
        }

        let totalLeftoverSlots = 0;
        const leftovers = this.materials.map(m => {
            const cons = consumed[m.name] || 0;
            const remaining = Math.max(0, (initialMap[m.name] || 0) - cons);
            const slots = calculateMatSlots(remaining);
            totalLeftoverSlots += slots;
            return {
                name: m.name,
                remaining: remaining,
                slots: slots,
                consumed: cons
            };
        });

        const afterSlots = totalTrapSlots + totalLeftoverSlots;
        const slotsSaved = beforeSlots - afterSlots;
        const reductionPercent = beforeSlots > 0 ? (slotsSaved / beforeSlots) * 100 : 0;

        return {
            status: "SUCCESS",
            beforeSlots: beforeSlots,
            afterSlots: afterSlots,
            slotsSaved: slotsSaved,
            reductionPercent: Math.max(0, reductionPercent),
            trapsToCraft: trapsToCraft,
            leftovers: leftovers,
            totalTrapsCrafted: totalTrapsCrafted
        };
    }

    /**
     * Greedy / local-search slot-compression optimizer fallback.
     */
    solveWithHeuristic(initialMap, inputWeights, beforeSlots) {
        const remaining = { ...initialMap };
        const craftCounts = {};
        for (const trap of this.recipes) craftCounts[trap.id] = 0;

        let improved = true;
        const activeRecipes = this.recipes.filter(t => (inputWeights[t.id] ?? t.defaultWeight) > 0);

        while (improved) {
            improved = false;
            let bestTrap = null;
            let bestDeltaSlots = 0;
            let bestScore = -Infinity;

            for (const trap of activeRecipes) {
                const weight = inputWeights[trap.id] ?? trap.defaultWeight;
                // Check if can craft at least 1 stack (or up to 200)
                let canCraft = Infinity;
                for (const [matName, cost] of Object.entries(trap.ingredients)) {
                    canCraft = Math.min(canCraft, Math.floor((remaining[matName] || 0) / cost));
                }

                if (canCraft <= 0) continue;

                // Test batches (e.g., 200, or remainder to fill current trap stack)
                const currentTrapCount = craftCounts[trap.id] || 0;
                const spaceInCurrentTrapSlot = (200 - (currentTrapCount % 200)) % 200;
                const testAmounts = [];
                if (spaceInCurrentTrapSlot > 0 && spaceInCurrentTrapSlot <= canCraft) {
                    testAmounts.push(spaceInCurrentTrapSlot);
                }
                if (canCraft >= 200) testAmounts.push(200);
                testAmounts.push(Math.min(canCraft, 50));
                testAmounts.push(1);

                for (const batch of testAmounts) {
                    if (batch <= 0 || batch > canCraft) continue;

                    // Compute slot delta
                    let matSlotsBefore = 0;
                    let matSlotsAfter = 0;

                    for (const [matName, cost] of Object.entries(trap.ingredients)) {
                        const cur = remaining[matName] || 0;
                        matSlotsBefore += calculateMatSlots(cur);
                        matSlotsAfter += calculateMatSlots(cur - cost * batch);
                    }

                    const trapSlotsBefore = calculateTrapSlots(currentTrapCount);
                    const trapSlotsAfter = calculateTrapSlots(currentTrapCount + batch);

                    const deltaSlots = (matSlotsBefore - matSlotsAfter) - (trapSlotsAfter - trapSlotsBefore);
                    const score = (deltaSlots * 1000) + (weight * batch * 0.01);

                    if (deltaSlots > 0 && score > bestScore) {
                        bestScore = score;
                        bestTrap = { trap, batch };
                        bestDeltaSlots = deltaSlots;
                    }
                }
            }

            if (bestTrap && bestDeltaSlots > 0) {
                const { trap, batch } = bestTrap;
                craftCounts[trap.id] += batch;
                for (const [matName, cost] of Object.entries(trap.ingredients)) {
                    remaining[matName] -= cost * batch;
                }
                improved = true;
            }
        }

        const trapsToCraft = [];
        let totalTrapsCrafted = 0;
        let totalTrapSlots = 0;
        const consumed = {};

        for (const trap of this.recipes) {
            const qty = craftCounts[trap.id] || 0;
            if (qty > 0) {
                const slots = calculateTrapSlots(qty);
                totalTrapsCrafted += qty;
                totalTrapSlots += slots;
                trapsToCraft.push({
                    id: trap.id,
                    name: trap.name,
                    category: trap.category,
                    quantity: qty,
                    slots: slots
                });

                for (const [matName, cost] of Object.entries(trap.ingredients)) {
                    consumed[matName] = (consumed[matName] || 0) + (cost * qty);
                }
            }
        }

        let totalLeftoverSlots = 0;
        const leftovers = this.materials.map(m => {
            const cons = consumed[m.name] || 0;
            const rem = Math.max(0, (initialMap[m.name] || 0) - cons);
            const slots = calculateMatSlots(rem);
            totalLeftoverSlots += slots;
            return {
                name: m.name,
                remaining: rem,
                slots: slots,
                consumed: cons
            };
        });

        const afterSlots = totalTrapSlots + totalLeftoverSlots;
        const slotsSaved = beforeSlots - afterSlots;
        const reductionPercent = beforeSlots > 0 ? (slotsSaved / beforeSlots) * 100 : 0;

        return {
            status: "SUCCESS",
            beforeSlots: beforeSlots,
            afterSlots: afterSlots,
            slotsSaved: slotsSaved,
            reductionPercent: Math.max(0, reductionPercent),
            trapsToCraft: trapsToCraft,
            leftovers: leftovers,
            totalTrapsCrafted: totalTrapsCrafted
        };
    }
}
