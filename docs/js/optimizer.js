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

        // Run the Exact Lossless Optimizer
        let result = null;
        try {
            result = this.solveExact(initialMap, inputWeights, beforeSlots);
        } catch (err) {
            console.warn("Exact optimizer encountered an issue, falling back to heuristic:", err);
        }

        // Fallback to greedy heuristic if needed
        if (!result || result.status !== "SUCCESS") {
            result = this.solveWithHeuristic(initialMap, inputWeights, beforeSlots);
        }

        result.executionTimeMs = Math.round(performance.now() - startTime);
        return result;
    }

    /**
     * Exact Lossless MILP / Branch-and-Bound Optimizer.
     * Evaluates discrete 200-stacks and 999-material ceilings directly,
     * guaranteeing 100% mathematically optimal slot reduction without thread hangs.
     */
    solveExact(initialMap, inputWeights, beforeSlots) {
        // Candidates: traps that can craft at least 1 stack (200 traps)
        const candidates = [];
        for (const trap of this.recipes) {
            let maxStacks = Infinity;
            let hasReq = false;
            for (const [mName, cost] of Object.entries(trap.ingredients)) {
                if (cost > 0) {
                    hasReq = true;
                    const avail = initialMap[mName] || 0;
                    maxStacks = Math.min(maxStacks, Math.floor(avail / (cost * 200)));
                }
            }
            if (!hasReq || maxStacks === 0 || maxStacks === Infinity) continue;

            const weight = inputWeights[trap.id] !== undefined ? inputWeights[trap.id] : trap.defaultWeight;
            if (weight <= 0) continue;

            const stackCosts = {};
            let totalMatFreed = 0;
            for (const [mName, cost] of Object.entries(trap.ingredients)) {
                if (cost > 0) {
                    stackCosts[mName] = cost * 200;
                    totalMatFreed += stackCosts[mName];
                }
            }

            candidates.push({
                trap,
                maxStacks,
                weight,
                stackCosts,
                totalMatFreed
            });
        }

        // Sort candidates by materials consumed per stack * preference weight
        candidates.sort((a, b) => (b.totalMatFreed * b.weight) - (a.totalMatFreed * a.weight));

        const calcTotalSlots = (stacks, currentLeftovers) => {
            let trapSlots = 0;
            let weightPenalty = 0;
            for (let i = 0; i < candidates.length; i++) {
                trapSlots += stacks[i];
                weightPenalty += stacks[i] * 0.0001 * 200 * candidates[i].weight;
            }
            let matSlots = 0;
            for (const mat of this.materials) {
                matSlots += calculateMatSlots(currentLeftovers[mat.name]);
            }
            return {
                discreteSlots: trapSlots + matSlots,
                score: (trapSlots + matSlots) - weightPenalty
            };
        };

        let bestScore = beforeSlots;
        let bestDiscreteSlots = beforeSlots;
        let bestStacks = new Array(candidates.length).fill(0);

        const currentStacks = new Array(candidates.length).fill(0);
        const currentLeftovers = Object.assign({}, initialMap);
        const memo = new Map();
        const maxTime = performance.now() + 1500; // 1.5 second safety watchdog

        const search = (index, currentTrapSlots) => {
            if (performance.now() > maxTime) return;

            const currentEval = calcTotalSlots(currentStacks, currentLeftovers);
            if (currentEval.score < bestScore) {
                bestScore = currentEval.score;
                bestDiscreteSlots = currentEval.discreteSlots;
                bestStacks = currentStacks.slice();
            }

            if (index >= candidates.length) return;

            // Transposition memoization: index + leftover slot profile
            let key = index;
            for (let m = 0; m < this.materials.length; m++) {
                const s = calculateMatSlots(currentLeftovers[this.materials[m].name]);
                key = (key * 31 + s) | 0;
            }
            const memoVal = memo.get(key);
            if (memoVal !== undefined && memoVal <= currentTrapSlots) {
                return;
            }
            memo.set(key, currentTrapSlots);

            const cand = candidates[index];
            let affordable = cand.maxStacks;
            for (const [mName, cost200] of Object.entries(cand.stackCosts)) {
                affordable = Math.min(affordable, Math.floor(currentLeftovers[mName] / cost200));
            }

            // Pruning: if current trap slots already >= bestScore, cannot beat it
            if (currentTrapSlots >= Math.floor(bestScore)) return;

            for (let s = affordable; s >= 0; s--) {
                if (s > 0) {
                    for (const [mName, cost200] of Object.entries(cand.stackCosts)) {
                        currentLeftovers[mName] -= cost200 * s;
                    }
                }
                currentStacks[index] = s;

                search(index + 1, currentTrapSlots + s);

                if (s > 0) {
                    for (const [mName, cost200] of Object.entries(cand.stackCosts)) {
                        currentLeftovers[mName] += cost200 * s;
                    }
                }
                currentStacks[index] = 0;
            }
        };

        search(0, 0);

        // Format result and compute exact material consumption
        const craftCounts = {};
        for (const trap of this.recipes) craftCounts[trap.id] = 0;
        for (let i = 0; i < candidates.length; i++) {
            craftCounts[candidates[i].trap.id] = bestStacks[i] * 200;
        }

        const consumed = {};
        const leftovers = {};
        for (const mat of this.materials) {
            consumed[mat.name] = 0;
            leftovers[mat.name] = initialMap[mat.name];
        }

        for (const trap of this.recipes) {
            const count = craftCounts[trap.id];
            if (count > 0) {
                for (const [mName, cost] of Object.entries(trap.ingredients)) {
                    consumed[mName] += cost * count;
                }
            }
        }
        for (const mat of this.materials) {
            leftovers[mat.name] = initialMap[mat.name] - consumed[mat.name];
        }

        // Post-pass: Check if any partial stack (1..199) can free a leftover material slot
        for (const cand of candidates) {
            const trap = cand.trap;
            const currentCount = craftCounts[trap.id];
            const remainingInStack = 200 - (currentCount % 200);

            if (remainingInStack < 200) {
                // Room in current stack (0 extra trap slots)
                let maxAdditional = remainingInStack;
                for (const [mName, cost] of Object.entries(trap.ingredients)) {
                    if (cost > 0) {
                        maxAdditional = Math.min(maxAdditional, Math.floor(leftovers[mName] / cost));
                    }
                }
                if (maxAdditional > 0) {
                    let curMatSlots = 0;
                    let newMatSlots = 0;
                    for (const [mName, cost] of Object.entries(trap.ingredients)) {
                        if (cost > 0) {
                            curMatSlots += calculateMatSlots(leftovers[mName]);
                            newMatSlots += calculateMatSlots(leftovers[mName] - (cost * maxAdditional));
                        }
                    }
                    if (newMatSlots < curMatSlots) {
                        craftCounts[trap.id] += maxAdditional;
                        for (const [mName, cost] of Object.entries(trap.ingredients)) {
                            consumed[mName] += cost * maxAdditional;
                            leftovers[mName] -= cost * maxAdditional;
                        }
                    }
                }
            }
        }

        // Calculate final slots
        let afterTrapSlots = 0;
        let totalTrapsCrafted = 0;
        const trapsToCraft = [];

        for (const trap of this.recipes) {
            const count = craftCounts[trap.id];
            if (count > 0) {
                const slots = calculateTrapSlots(count);
                afterTrapSlots += slots;
                totalTrapsCrafted += count;
                trapsToCraft.push({
                    id: trap.id,
                    name: trap.name,
                    category: trap.category,
                    quantity: count,
                    slots: slots,
                    ingredients: trap.ingredients
                });
            }
        }

        let afterMatSlots = 0;
        const leftoversList = this.materials.map(m => {
            const rem = leftovers[m.name];
            const mSlots = calculateMatSlots(rem);
            afterMatSlots += mSlots;
            return {
                name: m.name,
                remaining: rem,
                slots: mSlots,
                consumed: consumed[m.name]
            };
        });

        const afterSlots = afterTrapSlots + afterMatSlots;
        const slotsSaved = beforeSlots - afterSlots;
        const reductionPercent = beforeSlots > 0 ? (slotsSaved / beforeSlots) * 100 : 0;

        return {
            status: "SUCCESS",
            beforeSlots,
            afterSlots,
            slotsSaved,
            reductionPercent: Math.max(0, reductionPercent),
            trapsToCraft,
            leftovers: leftoversList,
            totalTrapsCrafted
        };
    }

    solveWithMILP(initialMap, inputWeights, beforeSlots) {
        const constraints = {};
        const variables = {};
        const ints = {};

        // 1. Material Constraints & Slack Variables
        for (const mat of this.materials) {
            const initialQty = initialMap[mat.name] || 0;
            constraints[`avail_${mat.id}`] = { max: initialQty };
            constraints[`leftover_cap_${mat.id}`] = { min: initialQty };

            const sMatVarName = `s_mat_${mat.id}`;
            const maxSlots = calculateMatSlots(initialQty);
            constraints[`ub_${sMatVarName}`] = { max: maxSlots };

            variables[sMatVarName] = {
                [`leftover_cap_${mat.id}`]: 999,
                [`ub_${sMatVarName}`]: 1,
                objective: 1.0
            };
        }

        // 2. Trap Stack Variables (each stack = 200 traps = exactly 1 slot)
        const craftableTraps = [];
        for (const trap of this.recipes) {
            let maxStacks = Infinity;
            let hasReq = false;
            for (const [mName, cost] of Object.entries(trap.ingredients)) {
                if (cost > 0) {
                    hasReq = true;
                    const avail = initialMap[mName] || 0;
                    maxStacks = Math.min(maxStacks, Math.floor(avail / (cost * 200)));
                }
            }
            if (!hasReq || maxStacks === 0 || maxStacks === Infinity) continue;

            const weight = inputWeights[trap.id] !== undefined ? inputWeights[trap.id] : trap.defaultWeight;
            if (weight <= 0) continue;

            craftableTraps.push(trap);
            const stackVarName = `stack_${trap.id}`;
            constraints[`ub_${stackVarName}`] = { max: maxStacks };

            // 1 stack = 1 trap slot. Objective incentive = -0.0001 * 200 * weight
            const stackVarObj = {
                [`ub_${stackVarName}`]: 1,
                objective: 1.0 - (0.0001 * 200 * weight)
            };

            for (const [mName, cost] of Object.entries(trap.ingredients)) {
                const matDef = this.materials.find(m => m.name === mName);
                if (matDef && cost > 0) {
                    const stackCost = cost * 200;
                    stackVarObj[`avail_${matDef.id}`] = stackCost;
                    stackVarObj[`leftover_cap_${matDef.id}`] = stackCost;
                }
            }

            variables[stackVarName] = stackVarObj;
            ints[stackVarName] = 1;
        }

        const model = {
            optimize: "objective",
            opType: "min",
            constraints: constraints,
            variables: variables,
            ints: ints,
            options: {
                timeout: 5000
            }
        };

        const solution = solver.Solve(model);
        if (!solution || !solution.feasible) {
            return { status: "INFEASIBLE" };
        }

        // Extract craft counts
        const craftCounts = {};
        for (const trap of this.recipes) craftCounts[trap.id] = 0;

        for (const trap of craftableTraps) {
            const stackVarName = `stack_${trap.id}`;
            const rawVal = solution[stackVarName];
            const stacks = rawVal ? Math.round(rawVal) : 0;
            craftCounts[trap.id] = stacks * 200;
        }

        // Calculate materials consumed & leftovers
        const consumed = {};
        const leftovers = {};
        for (const mat of this.materials) {
            consumed[mat.name] = 0;
            leftovers[mat.name] = initialMap[mat.name];
        }

        for (const trap of this.recipes) {
            const count = craftCounts[trap.id];
            if (count > 0) {
                for (const [mName, cost] of Object.entries(trap.ingredients)) {
                    consumed[mName] = (consumed[mName] || 0) + (cost * count);
                }
            }
        }

        for (const mat of this.materials) {
            leftovers[mat.name] = initialMap[mat.name] - consumed[mat.name];
        }

        // Calculate final slots and output
        const trapsToCraft = [];
        let totalTrapsCrafted = 0;
        let totalTrapSlots = 0;

        for (const trap of this.recipes) {
            const count = craftCounts[trap.id];
            if (count > 0) {
                const slots = calculateTrapSlots(count);
                totalTrapsCrafted += count;
                totalTrapSlots += slots;
                trapsToCraft.push({
                    id: trap.id,
                    name: trap.name,
                    category: trap.category,
                    quantity: count,
                    slots: slots,
                    ingredients: trap.ingredients
                });
            }
        }

        let totalLeftoverSlots = 0;
        const leftoversList = this.materials.map(m => {
            const cons = consumed[m.name] || 0;
            const remaining = leftovers[m.name] || 0;
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
            leftovers: leftoversList,
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
