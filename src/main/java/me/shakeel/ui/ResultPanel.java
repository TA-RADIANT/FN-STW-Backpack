package me.shakeel.ui;

import me.shakeel.model.Material;
import me.shakeel.model.OptimizationResult;
import me.shakeel.model.TrapRecipe;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Map;

/**
 * Bottom/Right Panel: Optimize Action Button and detailed itemized summary.
 * Displays Total Slots Before vs. Total Slots After, % Reduction, Traps to craft,
 * and leftover materials.
 */
public class ResultPanel extends JPanel {

    private final JButton optimizeButton;
    private final JLabel beforeSlotsValue = new JLabel("-", SwingConstants.CENTER);
    private final JLabel afterSlotsValue = new JLabel("-", SwingConstants.CENTER);
    private final JLabel savedSlotsValue = new JLabel("-", SwingConstants.CENTER);
    private final JLabel percentReductionValue = new JLabel("-", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Ready. Enter material counts and click Optimize Storage.");

    private final DefaultTableModel trapsTableModel;
    private final JTable trapsTable;
    private final DefaultTableModel leftoverTableModel;
    private final JTable leftoverTable;

    private OptimizationResult lastResult;

    public ResultPanel(Runnable onOptimizeClicked) {
        setLayout(new BorderLayout(8, 8));
        setBorder(ThemeUtils.createCardBorder("3. Optimization & Storage Results"));
        setBackground(ThemeUtils.CARD_BG);

        // Top: Large Action Button and Metric Cards
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Optimize Button
        optimizeButton = new JButton("⚡ OPTIMIZE STORAGE");
        optimizeButton.setFont(ThemeUtils.FONT_HEADER);
        optimizeButton.setBackground(ThemeUtils.ACCENT_COLOR);
        optimizeButton.setForeground(Color.WHITE);
        optimizeButton.setFocusPainted(false);
        optimizeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        optimizeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        optimizeButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        optimizeButton.setPreferredSize(new Dimension(300, 46));
        optimizeButton.addActionListener(e -> {
            if (onOptimizeClicked != null) {
                onOptimizeClicked.run();
            }
        });

        topContainer.add(optimizeButton);
        topContainer.add(Box.createVerticalStrut(10));

        // 4 Metric Cards in a row: Before, After, Saved, % Reduction
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 8, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        metricsGrid.add(createMetricCard("BEFORE", beforeSlotsValue, ThemeUtils.WARNING_COLOR));
        metricsGrid.add(createMetricCard("AFTER", afterSlotsValue, ThemeUtils.ACCENT_COLOR));
        metricsGrid.add(createMetricCard("SAVED", savedSlotsValue, ThemeUtils.SUCCESS_COLOR));
        metricsGrid.add(createMetricCard("REDUCTION", percentReductionValue, ThemeUtils.SUCCESS_COLOR));

        topContainer.add(metricsGrid);
        topContainer.add(Box.createVerticalStrut(10));

        add(topContainer, BorderLayout.NORTH);

        // Center: Tabbed Pane for Traps to Craft & Leftover Materials
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeUtils.FONT_BOLD);

        // Tab 1: Traps to Craft Table
        String[] trapCols = {"Trap Name", "Category", "Quantity to Craft", "Slots Used (200/sl)"};
        trapsTableModel = new DefaultTableModel(trapCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        trapsTable = new JTable(trapsTableModel);
        styleTable(trapsTable);
        JScrollPane trapsScroll = new JScrollPane(trapsTable);
        trapsScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.BORDER_COLOR));
        tabbedPane.addTab("Traps to Craft", trapsScroll);

        // Tab 2: Leftover Materials Table
        String[] matCols = {"Material", "Remaining Qty", "Leftover Slots (999/sl)", "Consumed Qty"};
        leftoverTableModel = new DefaultTableModel(matCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        leftoverTable = new JTable(leftoverTableModel);
        styleTable(leftoverTable);
        JScrollPane leftoverScroll = new JScrollPane(leftoverTable);
        leftoverScroll.setBorder(BorderFactory.createLineBorder(ThemeUtils.BORDER_COLOR));
        tabbedPane.addTab("Leftover Materials", leftoverScroll);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom: Status Bar & Copy Results Button
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));

        statusLabel.setFont(ThemeUtils.FONT_SMALL);
        statusLabel.setForeground(ThemeUtils.TEXT_MUTED);
        bottomBar.add(statusLabel, BorderLayout.CENTER);

        JButton copyBtn = ThemeUtils.createStyledButton("Copy Summary", new Color(51, 65, 85), Color.WHITE);
        copyBtn.setFont(ThemeUtils.FONT_SMALL);
        copyBtn.addActionListener(e -> copySummaryToClipboard());
        bottomBar.add(copyBtn, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(ThemeUtils.HEADER_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(ThemeUtils.FONT_SMALL);
        titleLabel.setForeground(ThemeUtils.TEXT_MUTED);

        valueLabel.setFont(ThemeUtils.FONT_METRIC);
        valueLabel.setForeground(valueColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void styleTable(JTable table) {
        table.setFont(ThemeUtils.FONT_REGULAR);
        table.setRowHeight(24);
        table.setShowGrid(true);
        table.setGridColor(ThemeUtils.BORDER_COLOR);
        table.getTableHeader().setFont(ThemeUtils.FONT_BOLD);
        table.getTableHeader().setBackground(ThemeUtils.HEADER_BG);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public void setOptimizing(boolean optimizing) {
        optimizeButton.setEnabled(!optimizing);
        if (optimizing) {
            optimizeButton.setText("Optimizing Storage with OR-Tools SCIP...");
            statusLabel.setText("Solving MILP formulation...");
        } else {
            optimizeButton.setText("⚡ OPTIMIZE STORAGE");
        }
    }

    public void displayResult(OptimizationResult result) {
        this.lastResult = result;

        if (result.getStatus() == OptimizationResult.Status.ERROR ||
            result.getStatus() == OptimizationResult.Status.INFEASIBLE) {
            statusLabel.setText("Failed: " + result.getMessage());
            statusLabel.setForeground(Color.RED);
            return;
        }

        // Update Metric Cards
        beforeSlotsValue.setText(String.valueOf(result.getTotalSlotsBefore()));
        afterSlotsValue.setText(String.valueOf(result.getTotalSlotsAfter()));
        savedSlotsValue.setText(String.valueOf(result.getSlotsSaved()));
        percentReductionValue.setText(String.format("%.1f%%", result.getSlotReductionPercent()));

        // Populate Traps to Craft Table
        trapsTableModel.setRowCount(0);
        for (Map.Entry<TrapRecipe, Integer> entry : result.getTrapsToCraft().entrySet()) {
            TrapRecipe trap = entry.getKey();
            int qty = entry.getValue();
            int slots = result.getTrapSlots().getOrDefault(trap, TrapRecipe.calculateSlots(qty));

            trapsTableModel.addRow(new Object[]{
                    trap.getName(),
                    trap.getCategory(),
                    String.format("%,d", qty),
                    String.valueOf(slots)
            });
        }
        if (result.getTrapsToCraft().isEmpty()) {
            trapsTableModel.addRow(new Object[]{"(No traps crafted)", "-", "0", "0"});
        }

        // Populate Leftover Materials Table
        leftoverTableModel.setRowCount(0);
        for (Material mat : Material.values()) {
            int remaining = result.getLeftoverMaterials().getOrDefault(mat, 0);
            int slots = result.getLeftoverSlots().getOrDefault(mat, Material.calculateSlots(remaining));
            int consumed = result.getConsumedMaterials().getOrDefault(mat, 0);

            leftoverTableModel.addRow(new Object[]{
                    mat.getDisplayName(),
                    String.format("%,d", remaining),
                    String.valueOf(slots),
                    String.format("%,d", consumed)
            });
        }

        statusLabel.setForeground(ThemeUtils.TEXT_MUTED);
        statusLabel.setText(String.format(
                "Optimization complete (%d ms). Total slots reduced from %d to %d (%d saved, %.1f%%). Total traps crafted: %,d.",
                result.getExecutionTimeMs(),
                result.getTotalSlotsBefore(),
                result.getTotalSlotsAfter(),
                result.getSlotsSaved(),
                result.getSlotReductionPercent(),
                result.getTotalTrapsCrafted()
        ));
    }

    private void copySummaryToClipboard() {
        if (lastResult == null) {
            JOptionPane.showMessageDialog(this, "Please run optimization first!", "No Results", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== FORTNITE: SAVE THE WORLD TRAP OPTIMIZER ===\n");
        sb.append(String.format("Total Slots Before: %d\n", lastResult.getTotalSlotsBefore()));
        sb.append(String.format("Total Slots After:  %d\n", lastResult.getTotalSlotsAfter()));
        sb.append(String.format("Slots Saved:        %d (%.1f%% reduction)\n", lastResult.getSlotsSaved(), lastResult.getSlotReductionPercent()));
        sb.append(String.format("Total Traps:        %d\n\n", lastResult.getTotalTrapsCrafted()));

        sb.append("--- TRAPS TO CRAFT ---\n");
        for (Map.Entry<TrapRecipe, Integer> entry : lastResult.getTrapsToCraft().entrySet()) {
            sb.append(String.format(" - %s: %,d units (%d slots)\n",
                    entry.getKey().getName(),
                    entry.getValue(),
                    lastResult.getTrapSlots().getOrDefault(entry.getKey(), 0)));
        }

        sb.append("\n--- LEFTOVER MATERIALS ---\n");
        for (Material mat : Material.values()) {
            int rem = lastResult.getLeftoverMaterials().getOrDefault(mat, 0);
            int slots = lastResult.getLeftoverSlots().getOrDefault(mat, 0);
            if (rem > 0) {
                sb.append(String.format(" - %s: %,d remaining (%d slots)\n", mat.getDisplayName(), rem, slots));
            }
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, "Crafting summary copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
    }
}
