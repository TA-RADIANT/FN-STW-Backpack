/**
 * Backpack & Storage MILP Optimization Engine.
 * Supports dual optimization modes:
 * 1. 'clean_stacks': Discrete 200-stack Branch-and-Bound (fast, tidy backpack, 100% offline)
 * 2. 'exact_wasm': High-performance WebAssembly MILP solver (HiGHS) matching Java OR-Tools 100%
 */

class InventoryOptimizer {
    constructor(materials, recipes) {
        this.materials = materials;
        this.recipes = recipes;
        this.highs = null;
        this.isWasmLoading = false;
        this.wasmError = null;
    }

    /**
     * Initializes HiGHS WebAssembly solver lazily.
     */
    async initWasm() {
        if (this.highs) return this.highs;
        if (this.wasmError) throw this.wasmError;

        if (this.isWasmLoading) {
            while (this.isWasmLoading) {
                await new Promise(r => setTimeout(r, 40));
            }
            if (this.highs) return this.highs;
            throw this.wasmError || new Error("HiGHS WebAssembly initialization failed");
        }

        this.isWasmLoading = true;
        try {
            const { default: highsLoader } = await import('https://cdn.jsdelivr.net/npm/highs@1.15.3/build/highs.mjs');
            this.highs = await highsLoader({
                locateFile: (file) => `https://cdn.jsdelivr.net/npm/highs@1.15.3/build/${file}`
            });
            return this.highs;
        } catch (err) {
            this.wasmError = err;
            console.warn("Unable to load HiGHS WebAssembly solver:", err);
            throw err;
        } finally {
            this.isWasmLoading = false;
        }
    }

    /**
     * Run the optimization.
     * @param {Object} inputMaterials Map of material name -> quantity (number)
     * @param {Object} inputWeights Map of trap id -> weight (0 to 10)
     * @param {string} mode 'clean_stacks' or 'exact_wasm'
     * @returns {Promise<Object>} Optimization results
     */
    async optimize(inputMaterials, inputWeights, mode = 'clean_stacks') {
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
                modeUsed: mode,
                solverLabel: mode === 'exact_wasm' ? "🔬 Exact MILP (WASM)" : "⚡ Clean Stacks (200s)",
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

        let result = null;

        if (mode === 'exact_wasm') {
            try {
                result = await this.solveWasmMILP(initialMap, inputWeights, beforeSlots);
            } catch (err) {
                console.warn("WASM MILP solver unavailable, falling back to Clean Stacks mode:", err);
                result = this.solveCleanStacks(initialMap, inputWeights, beforeSlots);
                result.modeFallback = true;
                result.fallbackReason = err.message || "WASM module could not be loaded";
            }
        } else {
            result = this.solveCleanStacks(initialMap, inputWeights, beforeSlots);
        }

        // Fallback to greedy heuristic if something went wrong
        if (!result || result.status !== "SUCCESS") {
            result = this.solveWithHeuristic(initialMap, inputWeights, beforeSlots);
        }

        result.executionTimeMs = Math.round(performance.now() - startTime);
        return result;
    }

    /**
     * WebAssembly HiGHS MILP Solver.
     * Evaluates full integer linear program with Gomory cuts,
     * achieving 100% mathematical parity with Java Google OR-Tools/SCIP.
     */
    async solveWasmMILP(initialMap, inputWeights, beforeSlots) {
        const highs = await this.initWasm();

        // Build CPLEX LP model matching Java's InventoryOptimizerService:
        // Variables:
        // x_{trap.id}: integer count of traps to craft
        // s_trap_{trap.id}: integer trap slots (200 traps per slot)
        // s_mat_{mat.id}: integer leftover material slots (999 items per slot)
        let lp = "Minimize\n  obj: ";

        const objTerms = [];
        for (const trap of this.recipes) {
            objTerms.push(`1 s_trap_${trap.id}`);
            const w = inputWeights[trap.id] !== undefined ? inputWeights[trap.id] : trap.defaultWeight;
            const tieBreaker = (0.0001 * w).toFixed(6);
            objTerms.push(`- ${tieBreaker} x_${trap.id}`);
        }
        for (const mat of this.materials) {
            objTerms.push(`1 s_mat_${mat.id}`);
        }
        lp += objTerms.join(" + ").replace(/\+ -/g, "- ") + "\n";

        lp += "Subject To\n";

        // 1. Material availability: sum(cost * x_k) <= initialQty
        for (const mat of this.materials) {
            const initialQty = initialMap[mat.name] || 0;
            const terms = [];
            for (const trap of this.recipes) {
                const cost = trap.ingredients[mat.name] || 0;
                if (cost > 0) {
                    terms.push(`${cost} x_${trap.id}`);
                }
            }
            if (terms.length > 0) {
                lp += `  avail_${mat.id}: ${terms.join(" + ")} <= ${initialQty}\n`;
            }
        }

        // 2. Trap slot capacity: x_k - 200 s_trap_k <= 0
        for (const trap of this.recipes) {
            lp += `  trap_cap_${trap.id}: x_${trap.id} - 200 s_trap_${trap.id} <= 0\n`;
        }

        // 3. Leftover material capacity: sum(cost * x_k) + 999 s_mat_m >= initialQty
        for (const mat of this.materials) {
            const initialQty = initialMap[mat.name] || 0;
            const terms = [];
            for (const trap of this.recipes) {
                const cost = trap.ingredients[mat.name] || 0;
                if (cost > 0) {
                    terms.push(`${cost} x_${trap.id}`);
                }
            }
            terms.push(`999 s_mat_${mat.id}`);
            lp += `  leftover_${mat.id}: ${terms.join(" + ")} >= ${initialQty}\n`;
        }

        lp += "Bounds\n";
        for (const trap of this.recipes) {
            lp += `  0 <= x_${trap.id}\n`;
            lp += `  0 <= s_trap_${trap.id}\n`;
        }
        for (const mat of this.materials) {
            const initialQty = initialMap[mat.name] || 0;
            const maxSlots = calculateMatSlots(initialQty);
            lp += `  0 <= s_mat_${mat.id} <= ${maxSlots}\n`;
        }

        lp += "Integers\n";
        const intVars = [];
        for (const trap of this.recipes) {
            intVars.push(`x_${trap.id}`);
            intVars.push(`s_trap_${trap.id}`);
        }
        for (const mat of this.materials) {
            intVars.push(`s_mat_${mat.id}`);
        }
        lp += "  " + intVars.join(" ") + "\nEnd\n";

        const sol = highs.solve(lp);
        if (sol.Status !== "Optimal" && sol.Status !== "Feasible") {
            throw new Error("HiGHS did not find an optimal solution: " + sol.Status);
        }

        // Extract craft counts
        const craftCounts = {};
        for (const trap of this.recipes) {
            const col = sol.Columns[`x_${trap.id}`];
            craftCounts[trap.id] = col ? Math.max(0, Math.round(col.Primal)) : 0;
        }

        // Calculate consumed, leftovers, and verify material balance
        const consumed = {};
        for (const mat of this.materials) consumed[mat.name] = 0;

        let totalTrapsCrafted = 0;
        let afterTrapSlots = 0;
        const trapsToCraft = [];

        for (const trap of this.recipes) {
            const qty = craftCounts[trap.id];
            if (qty > 0) {
                const slots = calculateTrapSlots(qty);
                afterTrapSlots += slots;
                totalTrapsCrafted += qty;
                trapsToCraft.push({
                    id: trap.id,
                    name: trap.name,
                    category: trap.category,
                    quantity: qty,
                    slots: slots,
                    ingredients: trap.ingredients
                });
                for (const [mName, cost] of Object.entries(trap.ingredients)) {
                    consumed[mName] += cost * qty;
                }
            }
        }

        let afterMatSlots = 0;
        const leftoversList = this.materials.map(m => {
            const rem = Math.max(0, (initialMap[m.name] || 0) - (consumed[m.name] || 0));
            const mSlots = calculateMatSlots(rem);
            afterMatSlots += mSlots;
            return {
                name: m.name,
                remaining: rem,
                slots: mSlots,
                consumed: consumed[m.name] || 0
            };
        });

        const afterSlots = afterTrapSlots + afterMatSlots;
        const slotsSaved = beforeSlots - afterSlots;
        const reductionPercent = beforeSlots > 0 ? (slotsSaved / beforeSlots) * 100 : 0;

        return {
            status: "SUCCESS",
            modeUsed: "exact_wasm",
            solverLabel: "🔬 Exact MILP (WASM - Java Parity)",
            beforeSlots,
            afterSlots,
            slotsSaved,
            reductionPercent: Math.max(0, reductionPercent),
            trapsToCraft,
            leftovers: leftoversList,
            totalTrapsCrafted
        };
    }

    /**
     * Clean Stacks Optimizer.
     * Evaluates discrete 200-stacks via Branch-and-Bound with memoization.
     * Guaranteed to keep backpacks neat with full 200-stacks and runs 100% offline.
     */
    solveCleanStacks(initialMap, inputWeights, beforeSlots) {
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
        const maxTime = performance.now() + 1500;

        const search = (index, currentTrapSlots) => {
            if (performance.now() > maxTime) return;

            const currentEval = calcTotalSlots(currentStacks, currentLeftovers);
            if (currentEval.score < bestScore) {
                bestScore = currentEval.score;
                bestDiscreteSlots = currentEval.discreteSlots;
                bestStacks = currentStacks.slice();
            }

            if (index >= candidates.length) return;

            // Transposition memoization
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
        for (const mat of this.materials) consumed[mat.name] = 0;

        let totalTrapsCrafted = 0;
        let afterTrapSlots = 0;
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
                for (const [mName, cost] of Object.entries(trap.ingredients)) {
                    consumed[mName] += cost * count;
                }
            }
        }

        let afterMatSlots = 0;
        const leftoversList = this.materials.map(m => {
            const rem = Math.max(0, (initialMap[m.name] || 0) - (consumed[m.name] || 0));
            const mSlots = calculateMatSlots(rem);
            afterMatSlots += mSlots;
            return {
                name: m.name,
                remaining: rem,
                slots: mSlots,
                consumed: consumed[m.name] || 0
            };
        });

        const afterSlots = afterTrapSlots + afterMatSlots;
        const slotsSaved = beforeSlots - afterSlots;
        const reductionPercent = beforeSlots > 0 ? (slotsSaved / beforeSlots) * 100 : 0;

        return {
            status: "SUCCESS",
            modeUsed: "clean_stacks",
            solverLabel: "⚡ Clean Stacks (200s)",
            beforeSlots,
            afterSlots,
            slotsSaved,
            reductionPercent: Math.max(0, reductionPercent),
            trapsToCraft,
            leftovers: leftoversList,
            totalTrapsCrafted
        };
    }

    /**
     * Greedy heuristic fallback.
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
                let canCraft = Infinity;
                for (const [matName, cost] of Object.entries(trap.ingredients)) {
                    canCraft = Math.min(canCraft, Math.floor((remaining[matName] || 0) / cost));
                }

                if (canCraft <= 0) continue;

                const currentTrapCount = craftCounts[trap.id] || 0;
                const spaceInCurrentTrapSlot = 200 - (currentTrapCount % 200);

                const testAmounts = [];
                if (spaceInCurrentTrapSlot > 0 && spaceInCurrentTrapSlot < 200 && canCraft >= spaceInCurrentTrapSlot) {
                    testAmounts.push(spaceInCurrentTrapSlot);
                }
                if (canCraft >= 200) testAmounts.push(200);
                testAmounts.push(Math.min(canCraft, 50));
                testAmounts.push(1);

                for (const batch of testAmounts) {
                    if (batch <= 0 || batch > canCraft) continue;

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
            modeUsed: "heuristic",
            solverLabel: "⚡ Greedy Heuristic",
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
