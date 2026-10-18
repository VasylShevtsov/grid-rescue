package edu.whitman.ui;

import edu.whitman.app.GameController;
import java.awt.GraphicsEnvironment;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Desktop entry point. The model can run without loading this class. */
public final class GridRescueApplication {
    private GridRescueApplication() { }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Grid Rescue needs a graphical desktop. Run this command in a desktop session.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            configureAppearance();
            JFrame frame = new JFrame("Grid Rescue");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(scrollableMission(new MissionView(new GameController())));
            frame.pack();
            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int availableWidth = Math.max(1, screen.width - 40);
            int availableHeight = Math.max(1, screen.height - 40);
            frame.setMinimumSize(new Dimension(Math.min(1000, availableWidth), Math.min(650, availableHeight)));
            frame.setSize(Math.min(frame.getWidth(), availableWidth), Math.min(frame.getHeight(), availableHeight));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static JScrollPane scrollableMission(MissionView view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getVerticalScrollBar().setUnitIncrement(20);
        pane.getHorizontalScrollBar().setUnitIncrement(20);
        Theme.styleScrollPane(pane);
        return pane;
    }

    static void configureAppearance() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            // The installed default look and feel remains usable.
        }
        UIManager.put("Button.font", Theme.font(13, true));
        UIManager.put("Label.font", Theme.font(13, false));
        UIManager.put("ToolTip.font", Theme.font(12, false));
        UIManager.put("Button.disabledText", Theme.MUTED);
        UIManager.put("Panel.background", Theme.BACKGROUND);
        UIManager.put("ToolTip.background", Theme.PAPER);
        UIManager.put("ToolTip.foreground", Theme.INK);
    }
}
