# ⚡ Fortnite: Save the World - Backpack & Storage Optimizer

[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-Live%20Demo-brightgreen?logo=github)](https://github.com/)
[![Pure Client-Side](https://img.shields.io/badge/Web-100%25%20Client--Side-00d2ff?logo=javascript)](docs/index.html)
[![WebAssembly](https://img.shields.io/badge/Solver-WebAssembly%20MILP-654ff0?logo=webassembly)](docs/js/optimizer.js)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](pom.xml)

A high-performance inventory optimization engine designed to maximize free storage and backpack slots in **Fortnite: Save the World** by converting loose crafting materials into compact, high-value trap stacks.

Available both as a **zero-install Web Application** (ready for GitHub Pages or offline use) and a native **Java Swing Desktop Application**.

---

## 🌐 Web Application (Zero Install)

### Option 1: Live on GitHub Pages
To host on your own repository with GitHub Pages:
1. Go to your repository settings: **Settings → Pages**.
2. Under **Build and deployment → Source**, select **Deploy from a branch**.
3. Choose the **`main`** branch and the **`/docs`** folder, then click **Save**.
4. GitHub Pages will publish the live app at:  
   `https://<your-username>.github.io/<your-repo-name>/`

### Option 2: Download & Use 100% Offline
1. Download or clone this repository:
   ```bash
   git clone https://github.com/<your-username>/BackpackOptimiser.git
   ```
2. Open [`docs/index.html`](docs/index.html) directly in any web browser (Chrome, Brave, Firefox, Edge, Safari).
3. **No server, Node.js, or internet connection required.** Everything runs entirely client-side.

---

## ⚡ Key Features

* **Dual-Input Tactile Steppers:** Input your materials both by full **999 Stacks** and loose **+ Items** (e.g., 5 stacks + 340 items = 5,335 total). Numbers $\ge 999$ automatically roll into stacks.
* **Rapid Increment Controls:** Click **+1** / **−1** for precision, or **+100** / **−100** for bulk adjustments. *Hold **Shift** while clicking ±100 to increment/decrement by ±10.*
* **Visual Material Badges:** Every raw material shows its in-game icon and real-time slot usage (1 slot per 999 items).
* **Two Dedicated Optimization Modes:**
  * **⚡ Clean Stacks (200s):** Fast, pragmatic offline solver that crafts clean full 200-stacks to keep your backpack tidy, respecting your trap preference weights.
  * **🔬 Max Lossless (WASM):** Client-side WebAssembly Mixed-Integer Linear Program (MILP) powered by the HiGHS simplex engine. Computes the mathematically global optimal slot compression (100% mathematical parity with the Java OR-Tools solver).
* **Trap Preference Weights (0 – 10):** Set sliders to prioritize traps you need for missions or Endurance. Setting a weight to `0` excludes that trap from crafting in Clean Stacks mode.
* **One-Click Clipboard Export:** Copy an itemized breakdown formatted for Discord and Reddit showing traps crafted, leftover materials, and slots saved.

---

## 📐 How the Optimization Math Works

Fortnite: Save the World inventory slots are consumed by two factors:
1. **Raw Crafting Materials:** 1 backpack/storage slot per 999 items.
2. **Crafted Traps:** 1 backpack/storage slot per 200 traps.

Because crafting costs are non-linear and resources are shared across multiple recipes, the solver formulates this as a Mixed-Integer Linear Program (MILP):

$$\text{Minimize} \quad \sum_{k} s_{\text{trap}, k} + \sum_{m} s_{\text{mat}, m} - \sum_{k} \epsilon \cdot w_k \cdot x_k$$

**Subject to:**
1. **Material Availability:** $\sum_{k} R_{mk} \cdot x_k \le \text{Initial}_{m}$ (cannot consume more materials than you have)
2. **Trap Slot Capacity:** $x_k - 200 \cdot s_{\text{trap}, k} \le 0$ (every 200 traps requires 1 slot)
3. **Leftover Material Capacity:** $\sum_{k} R_{mk} \cdot x_k + 999 \cdot s_{\text{mat}, m} \ge \text{Initial}_{m}$ (remaining loose items take slots)
4. **Physical Bounds:** $0 \le x_k \le \min_{m} \lfloor \text{Initial}_m / R_{mk} \rfloor$

---

## 📋 17 Included Trap Recipes

| # | Trap Recipe | Category | Key Ingredients |
|---|-------------|----------|-----------------|
| 1 | **Anti-Air Trap** | Ceiling / Floor | Nuts 'n' Bolts, Planks, Quartz Crystal |
| 2 | **Broadside** | Wall | Planks, Mechanical Parts, Mineral Powder |
| 3 | **Ceiling Drop Trap / Wall Darts** | Ceiling / Wall | Planks, Mechanical Parts, Twine |
| 4 | **Ceiling Electric Field** | Ceiling | Nuts 'n' Bolts, Rough Ore, Batteries, Mineral Powder |
| 5 | **Ceiling Gas Trap** | Ceiling | Nuts 'n' Bolts, Fibrous Herbs, Bacon, Mineral Powder |
| 6 | **Ceiling Zapper** | Ceiling | Nuts 'n' Bolts, Batteries, Mechanical Parts |
| 7 | **Cozy Campfire** | Floor | Planks, Flower Petals, Twine |
| 8 | **Flame Grill / Floor Freeze Trap** | Floor | Rough Ore, Quartz Crystal, Mineral Powder |
| 9 | **Floor Launcher / Wall Launcher** | Floor / Wall | Rough Ore, Planks, Mineral Powder |
| 10 | **Healing Pad** | Floor | Flower Petals, Fibrous Herbs, Bacon |
| 11 | **Retractable Floor Spikes** | Floor | Nuts 'n' Bolts, Rough Ore, Mechanical Parts |
| 12 | **Sound Wall** | Wall | Rough Ore, Quartz Crystal, Mechanical Parts |
| 13 | **Tar Pit** | Floor | Rough Ore, Quartz Crystal, Mechanical Parts |
| 14 | **Wall Dynamo** | Wall | Nuts 'n' Bolts, Batteries, Mechanical Parts |
| 15 | **Wall Lights** | Wall | Planks, Quartz Crystal, Mechanical Parts |
| 16 | **Wall Spikes** | Wall | Planks, Duct Tape, Twine |
| 17 | **Wooden Floor Spikes** | Floor | Planks, Duct Tape, Twine |

---

## 🖥️ Desktop Java Application

For developers or users who prefer running the native Java Swing application:

### Prerequisites
* **Java JDK 17+** (JDK 21 or newer recommended)
* **Apache Maven 3.8+**

### Run Unit & Integration Tests:
```powershell
mvn test
```

### Launch Desktop GUI:
```powershell
mvn compile exec:java -Dexec.mainClass="me.shakeel.Main"
```

### Package into a Standalone JAR:
```powershell
mvn clean package
java -jar target/BackpackOptimiser-1.0-SNAPSHOT.jar
```

---

## 📂 Project Structure

```
BackpackOptimiser/
├── docs/                   # Web application for GitHub Pages
│   ├── css/
│   │   └── style.css       # Responsive dark-theme styling
│   ├── images/             # In-game material & resource icons (.webp)
│   ├── js/
│   │   ├── app.js          # DOM controller, inputs & event handlers
│   │   ├── optimizer.js    # MILP (WASM HiGHS) & Clean Stacks algorithms
│   │   ├── recipes.js      # Raw materials & 17 trap definitions
│   │   └── solver.js       # Offline simplex LP library
│   └── index.html          # Main web application entry point
├── src/                    # Java Swing desktop application source
│   ├── main/java/me/shakeel/
│   │   ├── Main.java
│   │   ├── model/          # Material & Trap data models
│   │   ├── service/        # OR-Tools MILP optimization engine
│   │   └── ui/             # Swing interface & tables
│   └── main/resources/     # CSV & JSON recipe datasets
├── index.html              # Root redirect to docs/index.html
├── pom.xml                 # Maven build configuration
└── README.md
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
