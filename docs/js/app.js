/**
 * Fortnite: STW Backpack & Storage Optimizer - Application Logic.
 * Handles DOM rendering, reactive state syncing, and user interactions.
 * Completely stateless per run for independent execution.
 */

document.addEventListener("DOMContentLoaded", () => {
    const optimizer = new InventoryOptimizer(MATERIALS, TRAP_RECIPES);

    // State (Fresh per run)
    const state = {
        materials: {}, // mat.name -> { stacks: 0, remainder: 0 }
        weights: {},   // trap.id -> number (default 5)
        lastResult: null
    };

    // DOM Elements
    const materialsTableBody = document.getElementById("materialsTableBody");
    const totalItemsStat = document.getElementById("totalItemsStat");
    const totalSlotsStat = document.getElementById("totalSlotsStat");
    const clearMatsBtn = document.getElementById("clearMatsBtn");

    const trapsListContainer = document.getElementById("trapsListContainer");
    const resetWeightsBtn = document.getElementById("resetWeightsBtn");

    const optimizeBtn = document.getElementById("optimizeBtn");
    const statusText = document.getElementById("statusText");
    const copySummaryBtn = document.getElementById("copySummaryBtn");

    const metricBefore = document.getElementById("metricBefore");
    const metricAfter = document.getElementById("metricAfter");
    const metricSaved = document.getElementById("metricSaved");
    const metricReduction = document.getElementById("metricReduction");

    const tabTrapsBtn = document.getElementById("tabTrapsBtn");
    const tabMatsBtn = document.getElementById("tabMatsBtn");
    const tabTrapsContent = document.getElementById("tabTrapsContent");
    const tabMatsContent = document.getElementById("tabMatsContent");

    const trapsTableBody = document.getElementById("trapsTableBody");
    const leftoversTableBody = document.getElementById("leftoversTableBody");

    // Initialize Material Rows with Tactile Stepper Controls
    function renderMaterials() {
        materialsTableBody.innerHTML = "";

        MATERIALS.forEach(mat => {
            const tr = document.createElement("tr");

            // Col 0: Resource Icon (replaces text to save space)
            const tdIcon = document.createElement("td");
            tdIcon.className = "mat-icon-cell";
            tdIcon.title = `${mat.name} (Max 999/slot)`;

            const img = document.createElement("img");
            img.src = mat.image;
            img.alt = mat.name;
            img.className = "mat-icon";
            img.title = `${mat.name} (Max 999/slot)`;
            tdIcon.appendChild(img);

            // Col 1: Full 999 Stacks Stepper [- Stacks + +5]
            const tdStacks = document.createElement("td");
            tdStacks.className = "center";

            const stackBox = document.createElement("div");
            stackBox.className = "stepper-box";

            const btnDecStack = document.createElement("button");
            btnDecStack.type = "button";
            btnDecStack.className = "step-btn";
            btnDecStack.textContent = "−";
            btnDecStack.title = "Decrease 1 stack (or 5 with Shift)";

            const inputStacks = document.createElement("input");
            inputStacks.type = "number";
            inputStacks.min = "0";
            inputStacks.max = "9999";
            inputStacks.className = "step-input";
            inputStacks.id = `input_stacks_${mat.id}`;
            inputStacks.value = state.materials[mat.name]?.stacks || 0;
            inputStacks.dataset.mat = mat.name;
            inputStacks.dataset.type = "stacks";

            const btnIncStack = document.createElement("button");
            btnIncStack.type = "button";
            btnIncStack.className = "step-btn";
            btnIncStack.textContent = "+";
            btnIncStack.title = "Increase 1 stack (or 5 with Shift)";

            const btnQuickStack = document.createElement("button");
            btnQuickStack.type = "button";
            btnQuickStack.className = "step-quick-btn";
            btnQuickStack.textContent = "+5";
            btnQuickStack.title = "Quickly add 5 stacks";

            stackBox.appendChild(btnDecStack);
            stackBox.appendChild(inputStacks);
            stackBox.appendChild(btnIncStack);
            stackBox.appendChild(btnQuickStack);
            tdStacks.appendChild(stackBox);

            // Col 2: Loose Items Stepper [-100 -1 Loose +1 +100]
            const tdRem = document.createElement("td");
            tdRem.className = "center";

            const remBox = document.createElement("div");
            remBox.className = "stepper-box";

            const btnDec100 = document.createElement("button");
            btnDec100.type = "button";
            btnDec100.className = "step-btn-100 loose-step-100";
            btnDec100.textContent = "−100";
            btnDec100.title = "Decrease 100 items (Hold Shift for 10)";

            const btnDec1 = document.createElement("button");
            btnDec1.type = "button";
            btnDec1.className = "step-btn";
            btnDec1.textContent = "−";
            btnDec1.title = "Decrease 1 item";

            const inputRem = document.createElement("input");
            inputRem.type = "number";
            inputRem.min = "0";
            inputRem.className = "step-input loose-input";
            inputRem.id = `input_rem_${mat.id}`;
            inputRem.value = state.materials[mat.name]?.remainder || 0;
            inputRem.dataset.mat = mat.name;
            inputRem.dataset.type = "remainder";
            inputRem.title = "Loose items (0 - 998). Numbers >= 999 automatically roll into stacks!";

            const btnInc1 = document.createElement("button");
            btnInc1.type = "button";
            btnInc1.className = "step-btn";
            btnInc1.textContent = "+";
            btnInc1.title = "Increase 1 item";

            const btnInc100 = document.createElement("button");
            btnInc100.type = "button";
            btnInc100.className = "step-btn-100 loose-step-100";
            btnInc100.textContent = "+100";
            btnInc100.title = "Increase 100 items (Hold Shift for 10)";

            remBox.appendChild(btnDec100);
            remBox.appendChild(btnDec1);
            remBox.appendChild(inputRem);
            remBox.appendChild(btnInc1);
            remBox.appendChild(btnInc100);
            tdRem.appendChild(remBox);

            // Col 3: Total & Slots Badge with Row Clear (x) button
            const tdBadge = document.createElement("td");
            tdBadge.className = "right";

            const totalCell = document.createElement("div");
            totalCell.className = "total-cell";

            const badgeSpan = document.createElement("span");
            badgeSpan.className = "slot-badge";
            badgeSpan.id = `badge_${mat.id}`;
            badgeSpan.textContent = "0 (0 sl)";

            const clearRowBtn = document.createElement("button");
            clearRowBtn.type = "button";
            clearRowBtn.className = "row-clear-btn";
            clearRowBtn.textContent = "×";
            clearRowBtn.title = `Reset ${mat.name} to 0`;

            totalCell.appendChild(badgeSpan);
            totalCell.appendChild(clearRowBtn);
            tdBadge.appendChild(totalCell);

            tr.appendChild(tdIcon);
            tr.appendChild(tdStacks);
            tr.appendChild(tdRem);
            tr.appendChild(tdBadge);

            materialsTableBody.appendChild(tr);

            // Event Listeners for Text Inputs
            inputStacks.addEventListener("input", onMaterialInput);
            inputRem.addEventListener("input", onMaterialInput);

            // Auto-select text on focus so user can immediately type without backspacing
            inputStacks.addEventListener("focus", () => inputStacks.select());
            inputRem.addEventListener("focus", () => inputRem.select());

            // Click Handlers for Stacks Stepper Buttons
            btnIncStack.addEventListener("click", (e) => {
                const delta = e.shiftKey ? 5 : 1;
                modifyStack(mat.name, delta);
            });
            btnDecStack.addEventListener("click", (e) => {
                const delta = e.shiftKey ? -5 : -1;
                modifyStack(mat.name, delta);
            });
            btnQuickStack.addEventListener("click", () => {
                modifyStack(mat.name, 5);
            });

            // Click Handlers for Loose Stepper Buttons (+1, -1, +100, -100 with Shift -> 10)
            btnInc1.addEventListener("click", () => {
                modifyRemainder(mat.name, 1);
            });
            btnDec1.addEventListener("click", () => {
                modifyRemainder(mat.name, -1);
            });
            btnInc100.addEventListener("click", (e) => {
                const delta = e.shiftKey ? 10 : 100;
                modifyRemainder(mat.name, delta);
            });
            btnDec100.addEventListener("click", (e) => {
                const delta = e.shiftKey ? -10 : -100;
                modifyRemainder(mat.name, delta);
            });

            clearRowBtn.addEventListener("click", () => {
                resetMaterial(mat.name);
            });
        });
    }

    // Dynamic Shift Key Detection: updates all loose ±100 buttons to show ±10 when Shift is held down
    window.addEventListener("keydown", (e) => {
        if (e.key === "Shift") {
            document.querySelectorAll(".loose-step-100").forEach(btn => {
                if (btn.textContent.includes("+")) {
                    btn.textContent = "+10";
                } else if (btn.textContent.includes("−") || btn.textContent.includes("-")) {
                    btn.textContent = "−10";
                }
            });
        }
    });

    window.addEventListener("keyup", (e) => {
        if (e.key === "Shift") {
            document.querySelectorAll(".loose-step-100").forEach(btn => {
                if (btn.textContent.includes("+")) {
                    btn.textContent = "+100";
                } else if (btn.textContent.includes("−") || btn.textContent.includes("-")) {
                    btn.textContent = "−100";
                }
            });
        }
    });

    function modifyStack(matName, delta) {
        if (!state.materials[matName]) {
            state.materials[matName] = { stacks: 0, remainder: 0 };
        }
        state.materials[matName].stacks = Math.max(0, (state.materials[matName].stacks || 0) + delta);
        const mat = MATERIALS.find(m => m.name === matName);
        if (mat) {
            const input = document.getElementById(`input_stacks_${mat.id}`);
            if (input) input.value = state.materials[matName].stacks;
        }
        updateMaterialCalculations();
    }

    function modifyRemainder(matName, delta) {
        if (!state.materials[matName]) {
            state.materials[matName] = { stacks: 0, remainder: 0 };
        }
        let total = (state.materials[matName].stacks * 999) + state.materials[matName].remainder + delta;
        total = Math.max(0, total);
        state.materials[matName].stacks = Math.floor(total / 999);
        state.materials[matName].remainder = total % 999;

        const mat = MATERIALS.find(m => m.name === matName);
        if (mat) {
            const stackInput = document.getElementById(`input_stacks_${mat.id}`);
            const remInput = document.getElementById(`input_rem_${mat.id}`);
            if (stackInput) stackInput.value = state.materials[matName].stacks;
            if (remInput) remInput.value = state.materials[matName].remainder;
        }
        updateMaterialCalculations();
    }

    function resetMaterial(matName) {
        if (!state.materials[matName]) return;
        state.materials[matName] = { stacks: 0, remainder: 0 };
        const mat = MATERIALS.find(m => m.name === matName);
        if (mat) {
            const stackInput = document.getElementById(`input_stacks_${mat.id}`);
            const remInput = document.getElementById(`input_rem_${mat.id}`);
            if (stackInput) stackInput.value = 0;
            if (remInput) remInput.value = 0;
        }
        updateMaterialCalculations();
    }

    // Material Input Handler (with auto-rollover for loose items >= 999)
    function onMaterialInput(e) {
        const matName = e.target.dataset.mat;
        const type = e.target.dataset.type;
        const val = Math.max(0, parseInt(e.target.value, 10) || 0);

        if (!state.materials[matName]) {
            state.materials[matName] = { stacks: 0, remainder: 0 };
        }

        if (type === "remainder" && val >= 999) {
            const extraStacks = Math.floor(val / 999);
            const remainder = val % 999;
            state.materials[matName].stacks = (state.materials[matName].stacks || 0) + extraStacks;
            state.materials[matName].remainder = remainder;

            const mat = MATERIALS.find(m => m.name === matName);
            if (mat) {
                const stackInput = document.getElementById(`input_stacks_${mat.id}`);
                if (stackInput) stackInput.value = state.materials[matName].stacks;
            }
            e.target.value = remainder;
        } else {
            state.materials[matName][type] = val;
        }

        updateMaterialCalculations();
    }

    // Calculate Totals & Update Badges
    function updateMaterialCalculations() {
        let totalItems = 0;
        let totalSlots = 0;

        MATERIALS.forEach(mat => {
            const mState = state.materials[mat.name] || { stacks: 0, remainder: 0 };
            const qty = (mState.stacks * 999) + mState.remainder;
            const slots = calculateMatSlots(qty);

            totalItems += qty;
            totalSlots += slots;

            const badge = document.getElementById(`badge_${mat.id}`);
            if (badge) {
                badge.textContent = `${qty.toLocaleString()} (${slots} sl)`;
                badge.style.color = slots > 0 ? "#cbd5e1" : "var(--text-muted)";
            }
        });

        totalItemsStat.textContent = `${totalItems.toLocaleString()} items`;
        totalSlotsStat.textContent = `${totalSlots} slots`;
    }

    function getMaterialTotals() {
        const totals = {};
        MATERIALS.forEach(mat => {
            const mState = state.materials[mat.name] || { stacks: 0, remainder: 0 };
            totals[mat.name] = (mState.stacks * 999) + mState.remainder;
        });
        return totals;
    }

    // Initialize Trap Preference Cards (Sorted Alphabetically)
    function renderTraps() {
        trapsListContainer.innerHTML = "";

        const sortedRecipes = [...TRAP_RECIPES].sort((a, b) => a.name.localeCompare(b.name));

        sortedRecipes.forEach(trap => {
            const card = document.createElement("div");
            card.className = "trap-card";

            const weightVal = state.weights[trap.id] !== undefined ? state.weights[trap.id] : trap.defaultWeight;

            // Line 1: Header
            const header = document.createElement("div");
            header.className = "trap-card-header";

            const titleArea = document.createElement("div");
            titleArea.className = "trap-title-area";

            const catBadge = document.createElement("span");
            catBadge.className = "trap-category";
            catBadge.textContent = `[${trap.category}]`;

            const nameLabel = document.createElement("span");
            nameLabel.className = "trap-name";
            nameLabel.textContent = trap.name;

            titleArea.appendChild(catBadge);
            titleArea.appendChild(nameLabel);

            const weightBadge = document.createElement("span");
            weightBadge.className = `weight-badge ${weightVal === 0 ? "zero" : ""}`;
            weightBadge.id = `weight_val_${trap.id}`;
            weightBadge.textContent = `Weight: ${weightVal}`;

            header.appendChild(titleArea);
            header.appendChild(weightBadge);

            // Line 2: Slider
            const sliderRow = document.createElement("div");
            sliderRow.className = "trap-slider-row";

            const slider = document.createElement("input");
            slider.type = "range";
            slider.min = "0";
            slider.max = "10";
            slider.step = "1";
            slider.value = weightVal;
            slider.className = "trap-slider";
            slider.dataset.trapId = trap.id;

            slider.addEventListener("input", (e) => {
                const val = parseInt(e.target.value, 10);
                state.weights[trap.id] = val;
                weightBadge.textContent = `Weight: ${val}`;
                weightBadge.className = `weight-badge ${val === 0 ? "zero" : ""}`;
            });

            sliderRow.appendChild(slider);

            // Line 3: Recipe Cost Row with Resource Icons
            const costRow = document.createElement("div");
            costRow.className = "trap-cost-row";

            const costLabel = document.createElement("span");
            costLabel.textContent = "Cost:";
            costLabel.style.marginRight = "3px";
            costLabel.style.color = "var(--text-muted)";
            costRow.appendChild(costLabel);

            Object.entries(trap.ingredients).forEach(([name, cost]) => {
                const matDef = MATERIALS.find(m => m.name === name);
                const chip = document.createElement("span");
                chip.className = "cost-item";
                chip.title = `${cost} ${name}`;

                if (matDef) {
                    const icon = document.createElement("img");
                    icon.src = matDef.image;
                    icon.alt = name;
                    icon.className = "cost-icon";
                    chip.appendChild(icon);
                }

                const qty = document.createElement("span");
                qty.textContent = cost;
                chip.appendChild(qty);

                costRow.appendChild(chip);
            });

            card.appendChild(header);
            card.appendChild(sliderRow);
            card.appendChild(costRow);

            trapsListContainer.appendChild(card);
        });
    }

    // Reset All Trap Weights to Default (5)
    if (resetWeightsBtn) {
        resetWeightsBtn.addEventListener("click", () => {
            TRAP_RECIPES.forEach(trap => {
                state.weights[trap.id] = 5;
            });
            renderTraps();
        });
    }

    // Solver Mode Selection (Default: clean_stacks)
    let currentMode = "clean_stacks";

    const modeCleanBtn = document.getElementById("modeCleanBtn");
    const modeWasmBtn = document.getElementById("modeWasmBtn");
    const currentModeBadge = document.getElementById("currentModeBadge");
    const solverModeCaption = document.getElementById("solverModeCaption");

    function setSolverMode(mode) {
        currentMode = mode;

        if (mode === "exact_wasm") {
            if (modeCleanBtn) modeCleanBtn.classList.remove("active");
            if (modeWasmBtn) modeWasmBtn.classList.add("active");
            if (currentModeBadge) currentModeBadge.textContent = "Exact WASM (Java Parity)";
            if (solverModeCaption) solverModeCaption.textContent = "Maximum compression: crafts partial batches (e.g. 79 Launchers) for 100% exact parity with Java OR-Tools.";
        } else {
            if (modeWasmBtn) modeWasmBtn.classList.remove("active");
            if (modeCleanBtn) modeCleanBtn.classList.add("active");
            if (currentModeBadge) currentModeBadge.textContent = "Clean 200-Stacks";
            if (solverModeCaption) solverModeCaption.textContent = "Fast & offline: crafts full 200-stacks to keep your backpack tidy.";
        }
    }

    if (modeCleanBtn && modeWasmBtn) {
        modeCleanBtn.addEventListener("click", () => setSolverMode("clean_stacks"));
        modeWasmBtn.addEventListener("click", () => setSolverMode("exact_wasm"));
        setSolverMode(currentMode);
    }

    // Optimization Trigger
    optimizeBtn.addEventListener("click", async () => {
        optimizeBtn.disabled = true;
        optimizeBtn.textContent = "⚙️ OPTIMIZING...";
        statusText.style.color = "var(--text-muted)";
        statusText.textContent = currentMode === "exact_wasm"
            ? "Solving with WebAssembly HiGHS MILP..."
            : "Solving with Clean Stacks optimizer...";

        // Yield execution so the browser paints "⚙️ OPTIMIZING..." before computation
        await new Promise(resolve => setTimeout(resolve, 50));

        // Keep "OPTIMIZING..." visible for at least 200ms on fast cached runs
        const minDisplayTimer = new Promise(resolve => setTimeout(resolve, 150));

        try {
            const totals = getMaterialTotals();
            const [result] = await Promise.all([
                optimizer.optimize(totals, state.weights, currentMode),
                minDisplayTimer
            ]);
            state.lastResult = result;

            displayResults(result);
        } catch (err) {
            console.error("Optimization failed:", err);
            statusText.textContent = "Optimization error: " + (err.message || err);
            statusText.style.color = "var(--danger)";
        } finally {
            optimizeBtn.disabled = false;
            optimizeBtn.textContent = "⚡ OPTIMIZE STORAGE";
        }
    });

    // Display Results in UI
    function displayResults(result) {
        if (result.status === "EMPTY") {
            metricBefore.textContent = "0";
            metricAfter.textContent = "0";
            metricSaved.textContent = "0";
            metricReduction.textContent = "0.0%";
            trapsTableBody.innerHTML = `<tr><td colspan="4" class="center" style="color:var(--text-muted);">(No traps crafted - inventory empty)</td></tr>`;
            leftoversTableBody.innerHTML = "";
            statusText.textContent = result.message;
            return;
        }

        if (result.status === "INFEASIBLE") {
            statusText.textContent = "Optimization infeasible. Please adjust inputs.";
            statusText.style.color = "var(--danger)";
            return;
        }

        // Metrics
        metricBefore.textContent = result.beforeSlots;
        metricAfter.textContent = result.afterSlots;
        metricSaved.textContent = result.slotsSaved;
        metricReduction.textContent = `${result.reductionPercent.toFixed(1)}%`;

        // Traps to Craft Table
        trapsTableBody.innerHTML = "";
        if (result.trapsToCraft.length === 0) {
            trapsTableBody.innerHTML = `<tr><td colspan="4" class="center" style="color:var(--text-muted);">(No traps crafted - crafting traps would increase slots)</td></tr>`;
        } else {
            result.trapsToCraft.forEach(trap => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="mat-name">${trap.name}</td>
                    <td class="center" style="color:var(--accent); font-size:0.75rem;">${trap.category}</td>
                    <td class="center" style="font-weight:700;">${trap.quantity.toLocaleString()}</td>
                    <td class="right" style="color:var(--success); font-weight:700;">${trap.slots}</td>
                `;
                trapsTableBody.appendChild(tr);
            });
        }

        // Leftover Materials Table
        leftoversTableBody.innerHTML = "";
        result.leftovers.forEach(mat => {
            const matDef = MATERIALS.find(m => m.name === mat.name);
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="mat-name" style="display:flex; align-items:center; gap:6px;">
                    ${matDef ? `<img src="${matDef.image}" class="cost-icon" alt="${mat.name}">` : ""}
                    <span>${mat.name}</span>
                </td>
                <td class="center">${mat.remaining.toLocaleString()}</td>
                <td class="center" style="color:var(--warning); font-weight:600;">${mat.slots}</td>
                <td class="right" style="color:var(--text-muted);">${mat.consumed.toLocaleString()}</td>
            `;
            leftoversTableBody.appendChild(tr);
        });

        statusText.style.color = "var(--text-muted)";
        if (result.modeFallback) {
            statusText.innerHTML = `⚠️ <strong style="color:var(--warning);">WASM Offline Fallback:</strong> Solved with ${result.solverLabel} (${result.executionTimeMs} ms). Reduced slots from ${result.beforeSlots} to ${result.afterSlots} (${result.slotsSaved} saved).`;
        } else {
            statusText.innerHTML = `✓ <strong>${result.solverLabel}:</strong> Solved in ${result.executionTimeMs} ms. Reduced slots from ${result.beforeSlots} to ${result.afterSlots} (${result.slotsSaved} saved, ${result.reductionPercent.toFixed(1)}%). Crafted ${result.totalTrapsCrafted.toLocaleString()} traps.`;
        }
    }

    // Tabs switching
    tabTrapsBtn.addEventListener("click", () => {
        tabTrapsBtn.classList.add("active");
        tabMatsBtn.classList.remove("active");
        tabTrapsContent.classList.add("active");
        tabMatsContent.classList.remove("active");
    });

    tabMatsBtn.addEventListener("click", () => {
        tabMatsBtn.classList.add("active");
        tabTrapsBtn.classList.remove("active");
        tabMatsContent.classList.add("active");
        tabTrapsContent.classList.remove("active");
    });

    // Clear All Materials
    clearMatsBtn.addEventListener("click", () => {
        MATERIALS.forEach(mat => {
            state.materials[mat.name] = { stacks: 0, remainder: 0 };
        });
        renderMaterials();
        updateMaterialCalculations();
    });

    // Copy Summary to Clipboard
    copySummaryBtn.addEventListener("click", () => {
        if (!state.lastResult || state.lastResult.status !== "SUCCESS") {
            alert("Please run optimization first!");
            return;
        }

        const r = state.lastResult;
        const lines = [
            `🎮 Fortnite: Save the World - Backpack & Storage Optimization Summary`,
            `----------------------------------------------------------------------`,
            `Total Slots Before: ${r.beforeSlots}`,
            `Total Slots After:  ${r.afterSlots}`,
            `Slots Saved:        ${r.slotsSaved} (${r.reductionPercent.toFixed(1)}% reduction)`,
            `Total Traps:        ${r.totalTrapsCrafted.toLocaleString()}`,
            ``,
            `📦 Traps to Craft:`
        ];

        if (r.trapsToCraft.length === 0) {
            lines.push(`  (None)`);
        } else {
            r.trapsToCraft.forEach(t => {
                lines.push(`  • ${t.name} (${t.category}): ${t.quantity.toLocaleString()} crafted -> ${t.slots} slots`);
            });
        }

        lines.push(``);
        lines.push(`🪨 Leftover Materials:`);
        r.leftovers.forEach(m => {
            if (m.remaining > 0 || m.consumed > 0) {
                lines.push(`  • ${m.name}: ${m.remaining.toLocaleString()} remaining (${m.slots} slots) | ${m.consumed.toLocaleString()} consumed`);
            }
        });

        lines.push(`----------------------------------------------------------------------`);
        lines.push(`Generated by STW Backpack Optimizer (Web Edition)`);

        navigator.clipboard.writeText(lines.join("\n")).then(() => {
            const originalText = copySummaryBtn.textContent;
            copySummaryBtn.textContent = "✓ Copied!";
            setTimeout(() => { copySummaryBtn.textContent = originalText; }, 2000);
        }).catch(err => {
            console.error("Clipboard copy failed:", err);
            alert("Failed to copy to clipboard.");
        });
    });

    // App Startup (Clean slate every run)
    renderMaterials();
    renderTraps();
    updateMaterialCalculations();
});
