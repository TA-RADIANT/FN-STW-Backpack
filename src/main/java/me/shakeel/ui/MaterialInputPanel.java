package me.shakeel.ui;

import me.shakeel.model.AppState;
import me.shakeel.model.Material;
import me.shakeel.service.AppStateService;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Left Panel: Input controls for the 12 raw materials with slot badges,
 * Clear All, Random Test Data, and persistent custom material profiles.
 */
public class MaterialInputPanel extends JPanel {

    private final AppStateService appStateService;
    private final Map<Material, JSpinner> stackSpinners = new EnumMap<>(Material.class);
    private final Map<Material, JSpinner> remainderSpinners = new EnumMap<>(Material.class);
    private final Map<Material, JLabel> slotBadges = new EnumMap<>(Material.class);
    private final JLabel totalMatsLabel = new JLabel("0 items");
    private final JLabel totalSlotsLabel = new JLabel("0 slots");
    private final JComboBox<String> profileComboBox = new JComboBox<>();
    private final Random random = new Random();
    private boolean suppressAutoSave = false;

    public MaterialInputPanel(AppStateService appStateService) {
        this.appStateService = appStateService;

        setLayout(new BorderLayout(8, 8));
        setBorder(ThemeUtils.createCardBorder("1. Raw Materials (Max 999/slot)"));
        setBackground(ThemeUtils.CARD_BG);

        // Top: Profile Selector and Save/Delete
        JPanel topBar = new JPanel(new BorderLayout(6, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));

        JLabel profileLabel = new JLabel("Inventory Profile:");
        profileLabel.setFont(ThemeUtils.FONT_SMALL);
        profileLabel.setForeground(ThemeUtils.TEXT_MUTED);

        profileComboBox.setFont(ThemeUtils.FONT_SMALL);
        profileComboBox.addActionListener(e -> onProfileSelected());

        JButton saveProfileBtn = ThemeUtils.createStyledButton("Save...", new Color(14, 116, 144), Color.WHITE);
        saveProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        saveProfileBtn.addActionListener(e -> saveCustomProfile());

        JButton deleteProfileBtn = ThemeUtils.createStyledButton("Delete", new Color(100, 116, 139), Color.WHITE);
        deleteProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        deleteProfileBtn.addActionListener(e -> deleteSelectedProfile());

        JPanel profileBtnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        profileBtnGroup.setOpaque(false);
        profileBtnGroup.add(saveProfileBtn);
        profileBtnGroup.add(deleteProfileBtn);

        topBar.add(profileLabel, BorderLayout.WEST);
        topBar.add(profileComboBox, BorderLayout.CENTER);
        topBar.add(profileBtnGroup, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Grid for the 12 materials
        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Column Headers
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 4, 6, 4);

        gbc.gridx = 0;
        gbc.weightx = 0.36;
        JLabel matHeader = new JLabel("Material");
        matHeader.setFont(ThemeUtils.FONT_SMALL);
        matHeader.setForeground(ThemeUtils.TEXT_MUTED);
        gridPanel.add(matHeader, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.22;
        JLabel stackHeader = new JLabel("999 Stacks", SwingConstants.CENTER);
        stackHeader.setFont(ThemeUtils.FONT_SMALL);
        stackHeader.setForeground(ThemeUtils.TEXT_MUTED);
        gridPanel.add(stackHeader, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.22;
        JLabel remHeader = new JLabel("+ Remainder", SwingConstants.CENTER);
        remHeader.setFont(ThemeUtils.FONT_SMALL);
        remHeader.setForeground(ThemeUtils.TEXT_MUTED);
        gridPanel.add(remHeader, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.20;
        JLabel totalHeader = new JLabel("Total (Slots)", SwingConstants.RIGHT);
        totalHeader.setFont(ThemeUtils.FONT_SMALL);
        totalHeader.setForeground(ThemeUtils.TEXT_MUTED);
        gridPanel.add(totalHeader, gbc);

        int row = 1;
        for (Material mat : Material.values()) {
            gbc.gridy = row;
            gbc.insets = new Insets(3, 4, 3, 4);

            // Column 0: Material Name
            gbc.gridx = 0;
            gbc.weightx = 0.36;
            JLabel nameLabel = new JLabel(mat.getDisplayName());
            nameLabel.setFont(ThemeUtils.FONT_BOLD);
            gridPanel.add(nameLabel, gbc);

            // Column 1: Count of 999 stacks
            gbc.gridx = 1;
            gbc.weightx = 0.22;
            SpinnerNumberModel stackModel = new SpinnerNumberModel(0, 0, 9999, 1);
            JSpinner stackSpinner = new JSpinner(stackModel);
            stackSpinner.setFont(ThemeUtils.FONT_REGULAR);
            stackSpinner.setToolTipText("Count of full 999 stacks of " + mat.getDisplayName());
            JComponent stackEditor = stackSpinner.getEditor();
            if (stackEditor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) stackEditor).getTextField().setColumns(4);
                ((JSpinner.DefaultEditor) stackEditor).getTextField().setHorizontalAlignment(JTextField.CENTER);
            }
            stackSpinners.put(mat, stackSpinner);
            gridPanel.add(stackSpinner, gbc);

            // Column 2: Remaining stack (partial stack)
            gbc.gridx = 2;
            gbc.weightx = 0.22;
            SpinnerNumberModel remModel = new SpinnerNumberModel(0, 0, 999999, 10);
            JSpinner remSpinner = new JSpinner(remModel);
            remSpinner.setFont(ThemeUtils.FONT_REGULAR);
            remSpinner.setToolTipText("Remaining items in partial stack (0 - 998)");
            JComponent remEditor = remSpinner.getEditor();
            if (remEditor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) remEditor).getTextField().setColumns(4);
                ((JSpinner.DefaultEditor) remEditor).getTextField().setHorizontalAlignment(JTextField.CENTER);
            }
            remainderSpinners.put(mat, remSpinner);
            gridPanel.add(remSpinner, gbc);

            // Column 3: Total items & slots badge
            gbc.gridx = 3;
            gbc.weightx = 0.20;
            JLabel badge = new JLabel("0 (0 sl)", SwingConstants.RIGHT);
            badge.setFont(ThemeUtils.FONT_SMALL);
            badge.setForeground(ThemeUtils.TEXT_MUTED);
            slotBadges.put(mat, badge);
            gridPanel.add(badge, gbc);

            stackSpinner.addChangeListener(e -> {
                updateCalculations();
                if (!suppressAutoSave) {
                    saveCurrentMaterialsToState();
                }
            });
            remSpinner.addChangeListener(e -> {
                updateCalculations();
                if (!suppressAutoSave) {
                    saveCurrentMaterialsToState();
                }
            });

            row++;
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Controls and Summary
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        // Stats Box
        JPanel statsBox = new JPanel(new GridLayout(2, 2, 4, 2));
        statsBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeUtils.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 4, 6, 4)
        ));
        statsBox.setOpaque(false);

        JLabel totalMatsTitle = new JLabel("Total Materials:");
        totalMatsTitle.setFont(ThemeUtils.FONT_REGULAR);
        totalMatsTitle.setForeground(ThemeUtils.TEXT_MUTED);

        JLabel totalSlotsTitle = new JLabel("Initial Slots Used:");
        totalSlotsTitle.setFont(ThemeUtils.FONT_REGULAR);
        totalSlotsTitle.setForeground(ThemeUtils.TEXT_MUTED);

        totalMatsLabel.setFont(ThemeUtils.FONT_BOLD);
        totalSlotsLabel.setFont(ThemeUtils.FONT_BOLD);
        totalSlotsLabel.setForeground(ThemeUtils.WARNING_COLOR);

        statsBox.add(totalMatsTitle);
        statsBox.add(totalMatsLabel);
        statsBox.add(totalSlotsTitle);
        statsBox.add(totalSlotsLabel);

        bottomPanel.add(statsBox);
        bottomPanel.add(Box.createVerticalStrut(8));

        // Action Buttons: Clear All, Random Test Data
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
        btnRow.setOpaque(false);

        JButton clearBtn = ThemeUtils.createStyledButton("Clear All", new Color(71, 85, 105), Color.WHITE);
        clearBtn.addActionListener(e -> clearAll());

        JButton randomBtn = ThemeUtils.createStyledButton("Set Random Data", new Color(14, 116, 144), Color.WHITE);
        randomBtn.addActionListener(e -> setRandomTestData());

        btnRow.add(clearBtn);
        btnRow.add(randomBtn);

        bottomPanel.add(btnRow);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshProfileDropdown();
        loadSavedMaterials();
        updateCalculations();
    }

    public int getMaterialQuantity(Material mat) {
        JSpinner stackSp = stackSpinners.get(mat);
        JSpinner remSp = remainderSpinners.get(mat);
        if (stackSp == null || remSp == null) return 0;
        int stacks = ((Number) stackSp.getValue()).intValue();
        int rem = ((Number) remSp.getValue()).intValue();
        return Math.max(0, (stacks * Material.MAX_STACK_SIZE) + rem);
    }

    public Map<Material, Integer> getMaterialQuantities() {
        Map<Material, Integer> map = new EnumMap<>(Material.class);
        for (Material mat : Material.values()) {
            map.put(mat, getMaterialQuantity(mat));
        }
        return map;
    }

    public void setMaterialQuantity(Material mat, int quantity) {
        JSpinner stackSp = stackSpinners.get(mat);
        JSpinner remSp = remainderSpinners.get(mat);
        if (stackSp != null && remSp != null) {
            int safeQty = Math.max(0, quantity);
            int stacks = safeQty / Material.MAX_STACK_SIZE;
            int rem = safeQty % Material.MAX_STACK_SIZE;
            stackSp.setValue(stacks);
            remSp.setValue(rem);
        }
    }

    public void setAllMaterials(Map<Material, Integer> map) {
        suppressAutoSave = true;
        try {
            for (Material mat : Material.values()) {
                setMaterialQuantity(mat, map != null ? map.getOrDefault(mat, 0) : 0);
            }
        } finally {
            suppressAutoSave = false;
        }
        updateCalculations();
        saveCurrentMaterialsToState();
    }

    public void clearAll() {
        suppressAutoSave = true;
        try {
            for (Material mat : Material.values()) {
                stackSpinners.get(mat).setValue(0);
                remainderSpinners.get(mat).setValue(0);
            }
        } finally {
            suppressAutoSave = false;
        }
        updateCalculations();
        saveCurrentMaterialsToState();
    }

    public void setRandomTestData() {
        suppressAutoSave = true;
        try {
            for (Material mat : Material.values()) {
                int stacks = random.nextInt(15) + 1;
                int rem = random.nextInt(Material.MAX_STACK_SIZE);
                stackSpinners.get(mat).setValue(stacks);
                remainderSpinners.get(mat).setValue(rem);
            }
        } finally {
            suppressAutoSave = false;
        }
        updateCalculations();
        saveCurrentMaterialsToState();
    }

    private void updateCalculations() {
        int totalMats = 0;
        int totalSlots = 0;

        for (Material mat : Material.values()) {
            int qty = getMaterialQuantity(mat);
            int slots = Material.calculateSlots(qty);

            totalMats += qty;
            totalSlots += slots;

            JLabel badge = slotBadges.get(mat);
            if (badge != null) {
                badge.setText(String.format("%,d (%d sl)", qty, slots));
                badge.setForeground(slots > 0 ? ThemeUtils.TEXT_MUTED : new Color(100, 116, 139));
            }
        }

        totalMatsLabel.setText(String.format("%,d items", totalMats));
        totalSlotsLabel.setText(String.format("%d slots", totalSlots));
    }

    private void saveCurrentMaterialsToState() {
        if (appStateService != null) {
            AppState state = appStateService.getAppState();
            Map<String, Integer> map = new LinkedHashMap<>();
            for (Material mat : Material.values()) {
                map.put(mat.getDisplayName(), getMaterialQuantity(mat));
            }
            state.setCurrentMaterials(map);
            appStateService.saveState();
        }
    }

    private void loadSavedMaterials() {
        if (appStateService != null) {
            AppState state = appStateService.getAppState();
            Map<String, Integer> saved = state.getCurrentMaterials();
            if (saved != null && !saved.isEmpty()) {
                suppressAutoSave = true;
                try {
                    for (Map.Entry<String, Integer> entry : saved.entrySet()) {
                        try {
                            Material mat = Material.fromDisplayName(entry.getKey());
                            setMaterialQuantity(mat, entry.getValue());
                        } catch (Exception ignored) {
                        }
                    }
                } finally {
                    suppressAutoSave = false;
                }
            }
        }
    }

    public void refreshProfileDropdown() {
        profileComboBox.removeAllItems();
        profileComboBox.addItem("-- Select Inventory Profile --");
        if (appStateService != null) {
            for (String name : appStateService.getAppState().getMaterialProfiles().keySet()) {
                profileComboBox.addItem(name);
            }
        }
    }

    private void onProfileSelected() {
        int idx = profileComboBox.getSelectedIndex();
        if (idx <= 0) return;
        String name = (String) profileComboBox.getSelectedItem();
        if (name != null && appStateService != null) {
            Map<String, Integer> profile = appStateService.getAppState().getMaterialProfiles().get(name);
            if (profile != null) {
                suppressAutoSave = true;
                try {
                    clearAll();
                    for (Map.Entry<String, Integer> entry : profile.entrySet()) {
                        try {
                            Material mat = Material.fromDisplayName(entry.getKey());
                            setMaterialQuantity(mat, entry.getValue());
                        } catch (Exception ignored) {
                        }
                    }
                } finally {
                    suppressAutoSave = false;
                }
                updateCalculations();
                saveCurrentMaterialsToState();
            }
        }
    }

    private void saveCustomProfile() {
        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for this custom inventory profile:",
                "Save Inventory Profile",
                JOptionPane.QUESTION_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        name = name.trim();

        Map<String, Integer> current = new LinkedHashMap<>();
        for (Material mat : Material.values()) {
            current.put(mat.getDisplayName(), getMaterialQuantity(mat));
        }

        appStateService.getAppState().saveMaterialProfile(name, current);
        appStateService.saveState();

        refreshProfileDropdown();
        profileComboBox.setSelectedItem(name);
        JOptionPane.showMessageDialog(this, "Profile '" + name + "' saved successfully!", "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedProfile() {
        int idx = profileComboBox.getSelectedIndex();
        if (idx <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a custom profile to delete.", "Delete Profile", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = (String) profileComboBox.getSelectedItem();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete profile '" + name + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            appStateService.getAppState().removeMaterialProfile(name);
            appStateService.saveState();
            refreshProfileDropdown();
        }
    }
}
