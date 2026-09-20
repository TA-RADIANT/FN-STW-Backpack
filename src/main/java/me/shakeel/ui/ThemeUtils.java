package me.shakeel.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Styling and utility constants for the sleek dark gaming UI.
 */
public final class ThemeUtils {
    private ThemeUtils() {}

    // Gaming Aesthetic Palette
    public static final Color ACCENT_COLOR = new Color(59, 130, 246);      // Vibrant Electric Blue
    public static final Color ACCENT_HOVER = new Color(37, 99, 235);
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94);      // Vibrant Green
    public static final Color WARNING_COLOR = new Color(245, 158, 11);     // Amber
    public static final Color CARD_BG = new Color(30, 34, 45);             // Elevated card background
    public static final Color HEADER_BG = new Color(20, 24, 33);           // Dark header
    public static final Color BORDER_COLOR = new Color(51, 65, 85);        // Subtle dark border
    public static final Color TEXT_MUTED = new Color(148, 163, 184);       // Slate text

    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 22);

    public static Border createCardBorder(String title) {
        Border line = BorderFactory.createLineBorder(BORDER_COLOR, 1, true);
        Border titled = BorderFactory.createTitledBorder(
                line,
                "  " + title + "  ",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                FONT_TITLE,
                ACCENT_COLOR
        );
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 6, 6, 6),
                BorderFactory.createCompoundBorder(
                        titled,
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                )
        );
    }

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        return button;
    }
}
