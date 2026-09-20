package me.shakeel;

import com.formdev.flatlaf.FlatDarkLaf;
import me.shakeel.ui.MainFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class MainFrameTest {

    @Test
    @DisplayName("Verify MainFrame initialization and layout without errors")
    void testMainFrameInitialization() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment detected; skipping GUI frame test.");
            return;
        }

        SwingUtilities.invokeAndWait(() -> {
            FlatDarkLaf.setup();
            MainFrame frame = new MainFrame();
            assertNotNull(frame);
            assertEquals("Fortnite: Save the World - Backpack & Storage Optimizer", frame.getTitle());
            assertNotNull(frame.getContentPane());
            frame.dispose();
        });
    }
}
