package me.shakeel.ui;

import me.shakeel.model.Material;
import me.shakeel.model.OptimizationInput;
import me.shakeel.model.OptimizationResult;
import me.shakeel.model.TrapRecipe;
import me.shakeel.service.AppStateService;
import me.shakeel.service.InventoryOptimizerService;
import me.shakeel.service.RecipeRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

/**
 * Main Application Window integrating the 3 panels (Materials, Trap Preferences, Results)
 * with FlatLaf dark gaming theme and state persistence across executions.
 */
public class MainFrame extends JFrame {

    private final InventoryOptimizerService optimizerService;
    private final RecipeRepository recipeRepository;
    private final AppStateService appStateService;

    private final MaterialInputPanel materialInputPanel;
    private final TrapPreferencePanel trapPreferencePanel;
    private final ResultPanel resultPanel;

    public MainFrame() {
        super("Fortnite: Save the World - Backpack & Storage Optimizer");

        this.appStateService = new AppStateService();
        this.optimizerService = new InventoryOptimizerService();
        this.recipeRepository = new RecipeRepository();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1220, 780));
        setPreferredSize(new Dimension(1360, 860));

        // Create UI panels with state persistence
        this.materialInputPanel = new MaterialInputPanel(appStateService);
        this.trapPreferencePanel = new TrapPreferencePanel(recipeRepository.getRecipes(), appStateService);
        this.resultPanel = new ResultPanel(this::runOptimization);

        initLayout();
        pack();
        setLocationRelativeTo(null);

        // Auto-save state when window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                appStateService.saveState();
            }
        });
    }

    private void initLayout() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ThemeUtils.HEADER_BG);

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setBackground(ThemeUtils.HEADER_BG);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);

        JLabel mainTitle = new JLabel("FORTNITE: SAVE THE WORLD - TRAP & STORAGE OPTIMIZER");
        mainTitle.setFont(ThemeUtils.FONT_HEADER);
        mainTitle.setForeground(Color.WHITE);

        JLabel subTitle = new JLabel("Mixed-Integer Linear Program (MILP) using Google OR-Tools SCIP solver to minimize storage slots");
        subTitle.setFont(ThemeUtils.FONT_SMALL);
        subTitle.setForeground(ThemeUtils.TEXT_MUTED);

        titles.add(mainTitle);
        titles.add(subTitle);
        headerPanel.add(titles, BorderLayout.CENTER);

        // Header Actions (Quick Save State & Status Badge)
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);

        JButton saveStateBtn = ThemeUtils.createStyledButton("💾 Save State", new Color(14, 116, 144), Color.WHITE);
        saveStateBtn.setFont(ThemeUtils.FONT_SMALL);
        saveStateBtn.addActionListener(e -> {
            boolean ok = appStateService.saveState();
            if (ok) {
                JOptionPane.showMessageDialog(this, "Application state saved successfully to app_state.json!", "State Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JLabel badge = new JLabel("OR-Tools SCIP");
        badge.setFont(ThemeUtils.FONT_SMALL);
        badge.setForeground(ThemeUtils.ACCENT_COLOR);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.ACCENT_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        rightHeader.add(saveStateBtn);
        rightHeader.add(badge);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        root.add(headerPanel, BorderLayout.NORTH);

        // 3-Column Center Layout: Left (Materials), Center (Preferences), Right (Results)
        JPanel contentPanel = new JPanel(new GridLayout(1, 3, 8, 8));
        contentPanel.setBackground(ThemeUtils.HEADER_BG);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        contentPanel.add(materialInputPanel);
        contentPanel.add(trapPreferencePanel);
        contentPanel.add(resultPanel);

        root.add(contentPanel, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void runOptimization() {
        resultPanel.setOptimizing(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        Map<Material, Integer> materials = materialInputPanel.getMaterialQuantities();
        Map<TrapRecipe, Double> weights = trapPreferencePanel.getTrapWeights();
        OptimizationInput input = new OptimizationInput(materials, weights);
        List<TrapRecipe> recipes = recipeRepository.getRecipes();

        // Run solver on background thread using SwingWorker
        SwingWorker<OptimizationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected OptimizationResult doInBackground() {
                return optimizerService.optimize(input, recipes);
            }

            @Override
            protected void done() {
                try {
                    OptimizationResult result = get();
                    resultPanel.displayResult(result);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            MainFrame.this,
                            "Optimization error: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    resultPanel.setOptimizing(false);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        worker.execute();
    }
}
