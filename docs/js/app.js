/**
 * Fortnite: STW Backpack & Storage Optimizer - Application Logic.
 * Handles DOM rendering, reactive state syncing, localStorage persistence,
 * and user interactions.
 */

document.addEventListener("DOMContentLoaded", () => {
    const optimizer = new InventoryOptimizer(MATERIALS, TRAP_RECIPES);

    // State
    const state = {
        materials: {}, // mat.name -> { stacks: 0, remainder: 0 }
        weights: {},   // trap.id -> number
        profiles: {},  // profileName -> { mat.name: totalQty }
        lastResult: null
    };

    // DOM Elements
    const materialsTableBody = document.getElementById("materialsTableBody");
    const totalItemsStat = document.getElementById("totalItemsStat");
    const totalSlotsStat = document.getElementById("totalSlotsStat");
    const clearMatsBtn = document.getElementById("clearMatsBtn");
    const randomMatsBtn = document.getElementById("randomMatsBtn");
    const materialProfileSelect = document.getElementById("materialProfileSelect");
    const saveProfileBtn = document.getElementById("saveProfileBtn");
    const deleteProfileBtn = document.getElementById("deleteProfileBtn");

    const trapsListContainer = document.getElementById("trapsListContainer");
    const trapPresetSelect = document.getElementById("trapPresetSelect");

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

    // Initialize Material Rows
    function renderMaterials() {
        materialsTableBody.innerHTML = "";

        MATERIALS.forEach(mat => {
            const tr = document.createElement("tr");

            // Col 0: Name
            const tdName = document.createElement("td");
            tdName.className = "mat-name";
            tdName.textContent = mat.name;

            // Col 1: Stacks of 999
            const tdStacks = document.createElement("td");
            tdStacks.className = "center";
            const inputStacks = document.createElement("input");
            inputStacks.type = "number";
            inputStacks.min = "0";
            inputStacks.max = "9999";
            inputStacks.className = "num-input";
            inputStacks.value = state.materials[mat.name]?.stacks || 0;
            inputStacks.dataset.mat = mat.name;
            inputStacks.dataset.type = "stacks";
            inputStacks.title = `Number of full 999 stacks of ${mat.name}`;
            tdStacks.appendChild(inputStacks);

            // Col 2: Remainder
            const tdRem = document.createElement("td");
            tdRem.className = "center";
            const inputRem = document.createElement("input");
            inputRem.type = "number";
            inputRem.min = "0";
            inputRem.max = "999999";
            inputRem.className = "num-input";
            inputRem.value = state.materials[mat.name]?.remainder || 0;
            inputRem.dataset.mat = mat.name;
            inputRem.dataset.type = "remainder";
            inputRem.title = `Remaining items in partial stack (0 - 998)`;
            tdRem.appendChild(inputRem);

            // Col 3: Total & Slots Badge
            const tdBadge = document.createElement("td");
            tdBadge.className = "right slot-badge";
            tdBadge.id = `badge_${mat.id}`;
            tdBadge.textContent = "0 (0 sl)";

            tr.appendChild(tdName);
            tr.appendChild(tdStacks);
            tr.appendChild(tdRem);
            tr.appendChild(tdBadge);

            materialsTableBody.appendChild(tr);

            // Event listeners
            inputStacks.addEventListener("input", onMaterialInput);
            inputRem.addEventListener("input", onMaterialInput);
        });
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
                saveToLocalStorage();
            });

            sliderRow.appendChild(slider);

            // Line 3: Left-Aligned Recipe Cost Text
            const costRow = document.createElement("div");
            costRow.className = "trap-cost-row";

            const ingList = Object.entries(trap.ingredients)
                .map(([name, cost]) => `${cost} ${name}`)
                .join("  •  ");

            costRow.textContent = `Cost: ${ingList}`;

            card.appendChild(header);
            card.appendChild(sliderRow);
            card.appendChild(costRow);

            trapsListContainer.appendChild(card);
        });
    }

    // Material Input Handler
    function onMaterialInput(e) {
        const matName = e.target.dataset.mat;
        const type = e.target.dataset.type;
        const val = Math.max(0, parseInt(e.target.value, 10) || 0);

        if (!state.materials[matName]) {
            state.materials[matName] = { stacks: 0, remainder: 0 };
        }
        state.materials[matName][type] = val;

        updateMaterialCalculations();
        saveToLocalStorage();
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
                badge.style.color = slots > 0 ? "var(--text-muted)" : "#64748b";
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

    // Optimization Trigger
    optimizeBtn.addEventListener("click", () => {
        optimizeBtn.disabled = true;
        optimizeBtn.textContent = "⚙️ OPTIMIZING...";
        statusText.textContent = "Solving Mixed-Integer Linear Program...";

        setTimeout(() => {
            const totals = getMaterialTotals();
            const result = optimizer.optimize(totals, state.weights);
            state.lastResult = result;

            displayResults(result);

            optimizeBtn.disabled = false;
            optimizeBtn.textContent = "⚡ OPTIMIZE STORAGE";
        }, 30);
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
            trapsTableBody.innerHTML = `<tr><td colspan="4" class="center" style="color:var(--text-muted);">(No traps crafted)</td></tr>`;
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
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td class="mat-name">${mat.name}</td>
                <td class="center">${mat.remaining.toLocaleString()}</td>
                <td class="center" style="color:var(--warning); font-weight:600;">${mat.slots}</td>
                <td class="right" style="color:var(--text-muted);">${mat.consumed.toLocaleString()}</td>
            `;
            leftoversTableBody.appendChild(tr);
        });

        statusText.style.color = "var(--text-muted)";
        statusText.textContent = `Optimization complete (${result.executionTimeMs} ms). Reduced slots from ${result.beforeSlots} to ${result.afterSlots} (${result.slotsSaved} saved, ${result.reductionPercent.toFixed(1)}%). Crafted ${result.totalTrapsCrafted.toLocaleString()} traps.`;
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

    // Clear All
    clearMatsBtn.addEventListener("click", () => {
        MATERIALS.forEach(mat => {
            state.materials[mat.name] = { stacks: 0, remainder: 0 };
        });
        renderMaterials();
        updateMaterialCalculations();
        saveToLocalStorage();
    });

    // Random Data
    randomMatsBtn.addEventListener("click", () => {
        MATERIALS.forEach(mat => {
            const stacks = Math.floor(Math.random() * 15) + 1;
            const remainder = Math.floor(Math.random() * 999);
            state.materials[mat.name] = { stacks, remainder };
        });
        renderMaterials();
        updateMaterialCalculations();
        saveToLocalStorage();
    });

    // Trap Presets
    trapPresetSelect.addEventListener("change", (e) => {
        const val = e.target.value;
        if (!val) return;

        TRAP_RECIPES.forEach(trap => {
            if (val === "all_5") state.weights[trap.id] = 5;
            else if (val === "all_10") state.weights[trap.id] = 10;
            else if (val === "damage") {
                state.weights[trap.id] = trap.category.includes("Wall") || trap.category.includes("Ceiling") ? 8 : 2;
            } else if (val === "floor_heavy") {
                state.weights[trap.id] = trap.category.includes("Floor") ? 10 : 2;
            }
        });

        renderTraps();
        saveToLocalStorage();
        trapPresetSelect.value = "";
    });

    // Profile Management (localStorage)
    function refreshProfileDropdown() {
        materialProfileSelect.innerHTML = `<option value="">-- Select Inventory Profile --</option>`;
        Object.keys(state.profiles).forEach(name => {
            const opt = document.createElement("option");
            opt.value = name;
            opt.textContent = name;
            materialProfileSelect.appendChild(opt);
        });
    }

    saveProfileBtn.addEventListener("click", () => {
        const name = prompt("Enter a name for this custom inventory profile:");
        if (!name || !name.trim()) return;
        const trimmed = name.trim();

        const currentTotals = getMaterialTotals();
        state.profiles[trimmed] = currentTotals;
        saveToLocalStorage();
        refreshProfileDropdown();
        materialProfileSelect.value = trimmed;
        alert(`Profile "${trimmed}" saved!`);
    });

    deleteProfileBtn.addEventListener("click", () => {
        const selected = materialProfileSelect.value;
        if (!selected) {
            alert("Please select a profile to delete.");
            return;
        }
        if (confirm(`Are you sure you want to delete profile "${selected}"?`)) {
            delete state.profiles[selected];
            saveToLocalStorage();
            refreshProfileDropdown();
        }
    });

    materialProfileSelect.addEventListener("change", (e) => {
        const name = e.target.value;
        if (!name || !state.profiles[name]) return;

        const profileData = state.profiles[name];
        MATERIALS.forEach(mat => {
            const total = profileData[mat.name] || 0;
            state.materials[mat.name] = {
                stacks: Math.floor(total / 999),
                remainder: total % 999
            };
        });

        renderMaterials();
        updateMaterialCalculations();
        saveToLocalStorage();
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

    // LocalStorage State Persistence
    function saveToLocalStorage() {
        try {
            localStorage.setItem("stw_optimizer_state", JSON.stringify({
                materials: state.materials,
                weights: state.weights,
                profiles: state.profiles
            }));
        } catch (e) {
            console.warn("Could not save to localStorage:", e);
        }
    }

    function loadFromLocalStorage() {
        try {
            const raw = localStorage.getItem("stw_optimizer_state");
            if (raw) {
                const parsed = JSON.parse(raw);
                if (parsed.materials) state.materials = parsed.materials;
                if (parsed.weights) state.weights = parsed.weights;
                if (parsed.profiles) state.profiles = parsed.profiles;
            }
        } catch (e) {
            console.warn("Could not load from localStorage:", e);
        }
    }

    // App Startup
    loadFromLocalStorage();
    renderMaterials();
    renderTraps();
    updateMaterialCalculations();
    refreshProfileDropdown();
});
