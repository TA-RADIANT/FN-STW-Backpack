package me.shakeel.ui;

import me.shakeel.model.AppState;
import me.shakeel.model.Material;
import me.shakeel.model.TrapRecipe;
import me.shakeel.service.AppStateService;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Center Panel: Displays the 14 traps with full-width layout to avoid text cutoff,
 * preference weight sliders (0-10), and custom weight profile persistence.
 */
public class TrapPreferencePanel extends JPanel {

    private final List<TrapRecipe> recipes;
    private final AppStateService appStateService;
    private final Map<TrapRecipe, JSlider> sliders = new HashMap<>();
    private final Map<TrapRecipe, JLabel> valueLabels = new HashMap<>();
    private final JComboBox<String> profileComboBox = new JComboBox<>();

    public TrapPreferencePanel(List<TrapRecipe> recipes, AppStateService appStateService) {
        this.recipes = new java.util.ArrayList<>(recipes);
        this.recipes.sort(java.util.Comparator.comparing(TrapRecipe::getName, String.CASE_INSENSITIVE_ORDER));
        this.appStateService = appStateService;

        setLayout(new BorderLayout(8, 8));
        setBorder(ThemeUtils.createCardBorder("2. Trap Preference Weights (0 - 10)"));
        setBackground(ThemeUtils.CARD_BG);

        // Top Toolbar: Profiles and Quick Adjustment Buttons
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Profile Selector Row
        JPanel profileBar = new JPanel(new BorderLayout(6, 0));
        profileBar.setOpaque(false);
        profileBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));

        JLabel profileLabel = new JLabel("Weight Profile:");
        profileLabel.setFont(ThemeUtils.FONT_SMALL);
        profileLabel.setForeground(ThemeUtils.TEXT_MUTED);

        profileComboBox.setFont(ThemeUtils.FONT_SMALL);
        profileComboBox.addActionListener(e -> onProfileSelected());

        JButton saveProfileBtn = ThemeUtils.createStyledButton("Save Profile...", new Color(14, 116, 144), Color.WHITE);
        saveProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        saveProfileBtn.addActionListener(e -> saveCustomProfile());

        JButton deleteProfileBtn = ThemeUtils.createStyledButton("Delete", new Color(100, 116, 139), Color.WHITE);
        deleteProfileBtn.setFont(ThemeUtils.FONT_SMALL);
        deleteProfileBtn.addActionListener(e -> deleteSelectedProfile());

        JPanel profileBtnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        profileBtnGroup.setOpaque(false);
        profileBtnGroup.add(saveProfileBtn);
        profileBtnGroup.add(deleteProfileBtn);

        profileBar.add(profileLabel, BorderLayout.WEST);
        profileBar.add(profileComboBox, BorderLayout.CENTER);
        profileBar.add(profileBtnGroup, BorderLayout.EAST);

        // Quick Weights Toolbar Row
        JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        quickBar.setOpaque(false);

        JButton resetBtn = ThemeUtils.createStyledButton("Reset (5)", new Color(71, 85, 105), Color.WHITE);
        resetBtn.addActionListener(e -> setAllWeights(5));

        JButton maxBtn = ThemeUtils.createStyledButton("Max All (10)", new Color(14, 116, 144), Color.WHITE);
        maxBtn.addActionListener(e -> setAllWeights(10));

        JButton minBtn = ThemeUtils.createStyledButton("Zero All (0)", new Color(51, 65, 85), Color.WHITE);
        minBtn.addActionListener(e -> setAllWeights(0));

        quickBar.add(new JLabel("Quick Actions: "));
        quickBar.add(resetBtn);
        quickBar.add(maxBtn);
        quickBar.add(minBtn);

        topContainer.add(profileBar);
        topContainer.add(quickBar);
        topContainer.add(Box.createVerticalStrut(4));

        add(topContainer, BorderLayout.NORTH);

        // Trap Rows List
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        for (TrapRecipe trap : recipes) {
            JPanel row = createTrapCard(trap);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        refreshProfileDropdown();
        loadSavedWeights();
    }

    /**
     * Creates a vertical card for a trap to prevent the recipe text from being cut off by the slider bar.
     * Line 1: [Category] Trap Name  ---  Weight: X
     * Line 2: JSlider across full width
     * Line 3: Full recipe cost text across full width
     */
    private JPanel createTrapCard(TrapRecipe trap) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(ThemeUtils.HEADER_BG);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // Line 1: Header (Category + Name on Left, Weight Badge on Right)
        JPanel line1 = new JPanel(new BorderLayout(8, 0));
        line1.setOpaque(false);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);
        line1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftTitle.setOpaque(false);

        JLabel catBadge = new JLabel("[" + trap.getCategory() + "]");
        catBadge.setFont(ThemeUtils.FONT_SMALL);
        catBadge.setForeground(ThemeUtils.ACCENT_COLOR);

        JLabel nameLabel = new JLabel(trap.getName());
        nameLabel.setFont(ThemeUtils.FONT_BOLD);
        nameLabel.setForeground(Color.WHITE);

        leftTitle.add(catBadge);
        leftTitle.add(nameLabel);

        int defaultVal = (int) Math.round(trap.getDefaultWeight());
        JLabel valLabel = new JLabel("Weight: " + defaultVal, SwingConstants.RIGHT);
        valLabel.setFont(ThemeUtils.FONT_BOLD);
        valLabel.setForeground(ThemeUtils.SUCCESS_COLOR);

        line1.add(leftTitle, BorderLayout.WEST);
        line1.add(valLabel, BorderLayout.EAST);
        card.add(line1);

        // Line 2: Slider spanning full width
        JSlider slider = new JSlider(0, 10, defaultVal);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setOpaque(false);
        slider.setFont(ThemeUtils.FONT_SMALL);

        slider.addChangeListener(e -> {
            int val = slider.getValue();
            valLabel.setText("Weight: " + val);
            if (val == 0) valLabel.setForeground(ThemeUtils.TEXT_MUTED);
            else if (val >= 8) valLabel.setForeground(ThemeUtils.SUCCESS_COLOR);
            else valLabel.setForeground(Color.WHITE);
            saveCurrentWeightsToState();
        });

        sliders.put(trap, slider);
        valueLabels.put(trap, valLabel);

        card.add(Box.createVerticalStrut(2));
        card.add(slider);

        // Line 3: Recipe Ingredients (Full width, clearly readable, left-aligned)
        StringBuilder ingText = new StringBuilder("Cost: ");
        int count = 0;
        for (Map.Entry<Material, Integer> entry : trap.getIngredients().entrySet()) {
            if (count > 0) ingText.append("  •  ");
            ingText.append(entry.getValue()).append(" ").append(entry.getKey().getDisplayName());
            count++;
        }
        JLabel ingredientsLabel = new JLabel(ingText.toString(), SwingConstants.LEFT);
        ingredientsLabel.setFont(ThemeUtils.FONT_SMALL);
        ingredientsLabel.setForeground(ThemeUtils.TEXT_MUTED);
        ingredientsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        ingredientsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel recipeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        recipeRow.setOpaque(false);
        recipeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        recipeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        recipeRow.add(ingredientsLabel);

        card.add(Box.createVerticalStrut(2));
        card.add(recipeRow);

        return card;
    }

    public Map<TrapRecipe, Double> getTrapWeights() {
        Map<TrapRecipe, Double> weights = new HashMap<>();
        for (Map.Entry<TrapRecipe, JSlider> entry : sliders.entrySet()) {
            weights.put(entry.getKey(), (double) entry.getValue().getValue());
        }
        return weights;
    }

    public void setAllWeights(int weight) {
        int clamped = Math.max(0, Math.min(10, weight));
        for (JSlider slider : sliders.values()) {
            slider.setValue(clamped);
        }
        saveCurrentWeightsToState();
    }

    public void setWeightForTrap(String trapId, double weight) {
        for (Map.Entry<TrapRecipe, JSlider> entry : sliders.entrySet()) {
            if (entry.getKey().getId().equalsIgnoreCase(trapId)) {
                entry.getValue().setValue((int) Math.round(weight));
                break;
            }
        }
    }

    private void saveCurrentWeightsToState() {
        if (appStateService != null) {
            AppState state = appStateService.getAppState();
            Map<String, Double> map = new HashMap<>();
            for (Map.Entry<TrapRecipe, JSlider> entry : sliders.entrySet()) {
                map.put(entry.getKey().getId(), (double) entry.getValue().getValue());
            }
            state.setCurrentTrapWeights(map);
            appStateService.saveState();
        }
    }

    private void loadSavedWeights() {
        if (appStateService != null) {
            AppState state = appStateService.getAppState();
            Map<String, Double> saved = state.getCurrentTrapWeights();
            if (saved != null && !saved.isEmpty()) {
                for (Map.Entry<String, Double> entry : saved.entrySet()) {
                    setWeightForTrap(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void refreshProfileDropdown() {
        profileComboBox.removeAllItems();
        profileComboBox.addItem("-- Select Weight Profile --");
        if (appStateService != null) {
            for (String name : appStateService.getAppState().getWeightProfiles().keySet()) {
                profileComboBox.addItem(name);
            }
        }
    }

    private void onProfileSelected() {
        int idx = profileComboBox.getSelectedIndex();
        if (idx <= 0) return;
        String name = (String) profileComboBox.getSelectedItem();
        if (name != null && appStateService != null) {
            Map<String, Double> profile = appStateService.getAppState().getWeightProfiles().get(name);
            if (profile != null) {
                // If profile has a "default" key, set all to that first
                if (profile.containsKey("default")) {
                    setAllWeights((int) Math.round(profile.get("default")));
                }
                for (Map.Entry<String, Double> entry : profile.entrySet()) {
                    if (!entry.getKey().equalsIgnoreCase("default")) {
                        setWeightForTrap(entry.getKey(), entry.getValue());
                    }
                }
                saveCurrentWeightsToState();
            }
        }
    }

    private void saveCustomProfile() {
        String name = JOptionPane.showInputDialog(
                this,
                "Enter a name for this custom weight profile:",
                "Save Weight Profile",
                JOptionPane.QUESTION_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        name = name.trim();

        Map<String, Double> current = new HashMap<>();
        for (Map.Entry<TrapRecipe, JSlider> entry : sliders.entrySet()) {
            current.put(entry.getKey().getId(), (double) entry.getValue().getValue());
        }

        appStateService.getAppState().saveWeightProfile(name, current);
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
            appStateService.getAppState().removeWeightProfile(name);
            appStateService.saveState();
            refreshProfileDropdown();
        }
    }
}
