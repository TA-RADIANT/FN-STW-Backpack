package me.shakeel;

import com.formdev.flatlaf.FlatDarkLaf;
import me.shakeel.ui.MainFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Fortnite: Save the World Trap & Storage Optimizer.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure FlatLaf Dark theme
        try {
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.width", 12);
            FlatDarkLaf.setup();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize FlatDarkLaf, using system look and feel", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to launch main GUI", t);
                JOptionPane.showMessageDialog(
                        null,
                        "Fatal error launching application: " + t.getMessage(),
                        "Launch Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
