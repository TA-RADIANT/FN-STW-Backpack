# ⚡ Fortnite: Save the World - Backpack & Storage Optimizer

A Mixed-Integer Linear Programming (MILP) inventory optimization engine designed to maximize free storage and backpack slots in **Fortnite: Save the World** by converting raw crafting materials into compact, high-value trap stacks.

---

## 🌐 Instant Web Application (Zero Install / GitHub Pages)

You can run this application entirely in your browser with **zero installation**:

1. **Online (GitHub Pages):**  
   Enable GitHub Pages in your repository settings (**Settings → Pages → Source: `Deploy from a branch` → Select `main` and `/docs`**).
2. **Offline Local Use:**  
   Double-click [`index.html`](index.html) or [`docs/index.html`](docs/index.html) on your computer. It runs 100% locally and offline in Chrome, Firefox, Edge, or Safari without requiring Node.js, Java, or an internet connection.

---

## 🖥️ Desktop Java Application

For users who want to run the native Java Swing desktop application:

### Requirements:
- Java JDK 17+ (JDK 21 or 25 recommended)
- Maven

### Run Tests:
```powershell
.\mvn test
```

### Launch Desktop GUI:
```powershell
.\mvn compile exec:java -Dexec.mainClass="me.shakeel.Main"
```

---

## ✨ Features

- **Dual-Input Material Counts:** Input both your full **999 Stacks** count and your **Remainder** partial stack (e.g. 5 stacks + 340 remainder = 5,335 total items).
- **18 Precise Trap Recipes:** Synchronized with the latest Fortnite STW crafting costs, sorted alphabetically with left-aligned material costs.
- **Preference Sliders (0 - 10):** Direct the solver towards traps you actively use in your storm shield endurance or missions.
- **Mathematical Slot Guarantee:** Formulated as a Mixed-Integer Linear Program minimizing:
  $$\text{Total Slots} = \sum \text{Trap Slots (200/slot)} + \sum \text{Leftover Material Slots (999/slot)}$$
- **Itemized Breakdown & One-Click Copy:** View exact traps to craft, leftover materials, and copy a summary formatted for Discord and Reddit.
- **Profile Persistence:** Save custom inventory profiles locally so your farm runs and builds are always remembered.

---

## 📋 Trap Recipes Included

1. **Anti-Air Trap**
2. **Broadside**
3. **Ceiling Drop Trap / Wall Darts**
4. **Ceiling Electric Field**
5. **Ceiling Gas Trap**
6. **Ceiling Zapper**
7. **Cozy Campfire**
8. **Flame Grill Floor Trap / Floor Freeze Trap**
9. **Floor Launcher / Wall Launcher**
10. **Healing Pad**
11. **Jump Boost Pad**
12. **Retractable Floor Spikes**
13. **Sound Wall**
14. **Tar Pit**
15. **Wall Dynamo**
16. **Wall Lights**
17. **Wall Spikes**
18. **Wooden Floor Spikes**
