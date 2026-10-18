package edu.whitman.ui;

import edu.whitman.MissionStatus;
import edu.whitman.app.GameController;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;

/** Reproducible evidence from the real Swing view, using the same buttons as a player. */
public final class MissionScreenshots {
    private MissionScreenshots() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path directory = Path.of(args.length == 0 ? "docs/screenshots" : args[0]);
        Files.createDirectories(directory);
        SwingUtilities.invokeAndWait(() -> {
            try {
                GridRescueApplication.configureAppearance();
                GameController controller = new GameController();
                MissionView view = new MissionView(controller);
                capture(view, directory.resolve("mission-start.png"));
                MissionViewTest.click(view, "tile-0-0");
                capture(view, directory.resolve("action-rejected.png"));
                MissionViewTest.move(view, 4, 0, 3, 0);
                capture(view, directory.resolve("survivor-reached.png"));
                MissionViewTest.click(view, "pickup");
                capture(view, directory.resolve("survivor-aboard.png"));
                MissionViewTest.click(view, "restart");
                capture(view, directory.resolve("restart-confirmation.png"));
                MissionViewTest.click(view, "cancel-restart");
                MissionViewTest.move(view, 4, 1, 5, 2);
                MissionViewTest.click(view, "deliver");
                capture(view, directory.resolve("scout-upgraded.png"));
                MissionViewTest.finishMission(view);
                if (controller.getGame().getStatus() != MissionStatus.WON
                        || controller.getGame().getTurnsUsed() != 40) {
                    throw new IllegalStateException("The screenshot scenario did not win in 40 actions");
                }
                capture(view, directory.resolve("mission-won.png"));
                MissionViewTest.click(view, "play-again");
                MissionViewTest.click(view, "help");
                capture(view, directory.resolve("how-to-play.png"));
                capture(view, directory.resolve("how-to-play-compact.png"), 980, 760);
                JScrollPane helpScroll = (JScrollPane) MissionViewTest.find(view, "help-scroll");
                helpScroll.getVerticalScrollBar().setValue(helpScroll.getVerticalScrollBar().getMaximum());
                capture(view, directory.resolve("how-to-play-rules.png"), 980, 760);
                MissionViewTest.click(view, "close-help");
                capture(view, directory.resolve("mission-compact.png"), 980, 760);
                capture(view, directory.resolve("mission-large.png"), 1440, 980);
                GameController lostController = new GameController();
                MissionView lost = new MissionView(lostController);
                for (int action = 0; action < 79; action++) {
                    MissionViewTest.click(lost, action % 2 == 0 ? "tile-4-0" : "tile-5-0");
                }
                capture(lost, directory.resolve("mission-low-budget.png"));
                MissionViewTest.click(lost, "tile-5-0");
                if (lostController.getGame().getStatus() != MissionStatus.LOST) {
                    throw new IllegalStateException("The loss screenshot did not exhaust the action budget");
                }
                capture(lost, directory.resolve("mission-lost.png"));
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to create screenshot evidence", exception);
            }
        });
        System.out.println("Saved real Swing mission and interaction screenshots to " + directory.toAbsolutePath());
    }

    private static void capture(MissionView view, Path output) throws Exception {
        capture(view, output, 1160, 820);
    }

    private static void capture(MissionView view, Path output, int width, int height) throws Exception {
        view.setSize(width, height);
        invalidateTree(view);
        layout(view);
        invalidateTree(view);
        layout(view);
        BufferedImage image = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        view.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", output.toFile());
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) layout((Container) child);
        }
    }

    private static void invalidateTree(Container parent) {
        parent.invalidate();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) invalidateTree((Container) child);
        }
    }
}
