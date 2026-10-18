package edu.whitman.ui;

import edu.whitman.MissionStatus;
import edu.whitman.RescueGame;
import edu.whitman.RescueTeam;
import edu.whitman.Scout;
import edu.whitman.Terrain;
import edu.whitman.app.GameController;
import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual Swing buttons on the event thread without requiring a display. */
class MissionViewTest {
    @Test
    void smallerWindowsScrollInsteadOfOverlappingBoardText() throws Exception {
        onEventThread(() -> {
            MissionView view = new MissionView(new GameController());
            javax.swing.JScrollPane window = GridRescueApplication.scrollableMission(view);
            window.setSize(1000, 650);
            layout(window);
            layout(window);
            assertTrue(window.getVerticalScrollBar().isVisible());
            assertTrue(view.getHeight() >= 760);
            assertTrue(find(view, "tile-5-0").getHeight() >= 64);
            assertTrue(find(view, "tile-5-0").getWidth() >= 64);
        });
    }

    @Test
    void rejectedMoveAndUnavailablePickupKeepTheMissionPlayable() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            assertTrue(find(view, "tile-4-0").getAccessibleContext().getAccessibleName().contains("legal destination"));
            click(view, "tile-0-4");
            assertTrue(feedback(view).contains("legal neighboring destination"));
            assertEquals(0, controller.getGame().getTurnsUsed());
            assertFalse(find(view, "pickup").isEnabled());
            click(view, "pickup");
            assertEquals(0, controller.getGame().getTurnsUsed());
            move(view, 4, 0, 3, 0);
            assertEquals(2, controller.getGame().getTurnsUsed());
            assertTrue(((JLabel) find(view, "survivors")).getText().contains("Waiting at (3,0)"));
            click(view, "pickup");
            assertTrue(((JLabel) find(view, "survivors")).getText().contains("Aboard Scout"));
            assertTrue(((JLabel) find(view, "selected-details")).getText().contains("1 / 1 (D)"));
            assertFalse(controller.isError());
        });
    }

    @Test
    void fullMissionIsPlayableThroughButtonsAndLocksAtVictory() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            firstRescue(view);
            assertTrue(((JLabel) find(view, "selected-details")).getText().contains("Active: water access"));
            assertTrue(feedback(view).contains("upgraded"));
            finishMission(view);
            assertEquals(MissionStatus.WON, controller.getGame().getStatus());
            assertEquals(40, controller.getGame().getTurnsUsed());
            assertEquals(4, controller.getGame().getTeam().getRescuedCount());
            assertTrue(feedback(view).contains("Mission won"));
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            assertFalse(find(view, "tile-0-0").isEnabled());
            click(view, "tile-0-0");
            click(view, "pickup");
            click(view, "deliver");
            assertEquals(40, controller.getGame().getTurnsUsed());
            click(view, "select-Scout");
            assertTrue(controller.getSelectedUnit() instanceof Scout);
            assertTrue(feedback(view).contains("Mission won"));
            assertEquals("Legal destinations: none", ((JLabel) find(view, "destinations")).getText());
        });
    }

    @Test
    void lossIsVisibleAndNewMissionRebindsEveryControlToFreshState() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController(shortMission());
            RescueGame previous = controller.getGame();
            MissionView view = new MissionView(controller);
            click(view, "tile-0-1");
            assertEquals(MissionStatus.LOST, previous.getStatus());
            assertTrue(feedback(view).contains("Mission lost"));
            assertFalse(find(view, "tile-0-0").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            click(view, "restart");
            assertNotSame(previous, controller.getGame());
            assertEquals(80, controller.getGame().getTurnsRemaining());
            assertEquals(0, controller.getGame().getTurnsUsed());
            assertTrue(find(view, "tile-4-1").isEnabled());
            click(view, "select-Medic");
            click(view, "tile-4-1");
            assertSame(controller.getGame().getTile(4, 1), controller.getSelectedUnit().getTile());
            assertEquals(1, controller.getGame().getTurnsUsed());
            assertEquals(MissionStatus.LOST, previous.getStatus());
        });
    }

    @Test
    void restartingAnActiveMissionResetsCargoSelectionAndBoardLabels() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            move(view, 4, 0, 3, 0);
            click(view, "pickup");
            click(view, "restart");
            click(view, "confirm-restart");
            assertTrue(((JLabel) find(view, "selected-details")).getText().contains("0 / 1 (empty)"));
            assertTrue(find(view, "tile-3-0").getAccessibleContext().getAccessibleName().contains("survivor D"));
            assertTrue(find(view, "tile-5-0").getAccessibleContext().getAccessibleName().toLowerCase().contains("selected unit"));
            assertEquals(0, controller.getGame().getTurnsUsed());
        });
    }

    @Test
    void emptyScenarioHasHelpfulFeedbackAndFreshMissionRemainsAvailable() throws Exception {
        onEventThread(() -> {
            Terrain[][] terrain = new Terrain[6][5];
            for (Terrain[] row : terrain) Arrays.fill(row, Terrain.PLAIN);
            terrain[5][2] = Terrain.SHELTER;
            MissionView view = new MissionView(new GameController(new RescueGame(new RescueTeam("Empty"), terrain, 1)));
            assertFalse(find(view, "pickup").isEnabled());
            assertTrue(feedback(view).contains("no units"));
            click(view, "restart");
            assertFalse(find(view, "pickup").isEnabled());
            assertTrue(find(view, "tile-4-0").isEnabled());
            assertTrue(find(view, "select-CommandUnit").isEnabled());
        });
    }

    static RescueGame shortMission() {
        Terrain[][] terrain = new Terrain[6][5];
        for (Terrain[] row : terrain) Arrays.fill(row, Terrain.PLAIN);
        terrain[5][2] = Terrain.SHELTER;
        RescueGame game = new RescueGame(new RescueTeam("One action fixture"), terrain, 1);
        new Scout(game.getTeam(), game.getTile(0, 0));
        return game;
    }

    static void firstRescue(MissionView view) {
        move(view, 4, 0, 3, 0);
        click(view, "pickup");
        move(view, 4, 1, 5, 2);
        click(view, "deliver");
    }

    static void finishMission(MissionView view) {
        move(view, 4, 1, 3, 0, 2, 0, 1, 0, 0, 0);
        click(view, "pickup");
        move(view, 1, 0, 2, 0, 3, 0, 4, 1, 5, 2);
        click(view, "deliver");
        move(view, 4, 2);
        click(view, "select-RescueTruck");
        move(view, 4, 3, 3, 3, 2, 3, 2, 4);
        click(view, "pickup");
        move(view, 2, 3, 1, 3, 0, 3, 0, 4);
        click(view, "pickup");
        move(view, 0, 3, 1, 3, 2, 3, 3, 3, 4, 3, 5, 3, 5, 2);
        click(view, "deliver");
        move(view, 5, 3);
        click(view, "select-CommandUnit");
        move(view, 4, 3, 5, 2);
    }

    static void move(MissionView view, int... coordinates) {
        for (int i = 0; i < coordinates.length; i += 2) {
            click(view, "tile-" + coordinates[i] + "-" + coordinates[i + 1]);
            assertFalse(feedback(view).startsWith("Action rejected:"), feedback(view));
        }
    }

    static void click(Container view, String name) {
        ((JButton) find(view, name)).doClick(0);
    }

    static String feedback(MissionView view) {
        return ((JTextArea) find(view, "feedback")).getText();
    }

    static Component find(Container parent, String name) {
        Component found = findOrNull(parent, name);
        assertNotNull(found, "Missing control: " + name);
        return found;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) layout((Container) child);
        }
    }

    private static Component findOrNull(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container) {
                Component found = findOrNull((Container) child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    static void onEventThread(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                GridRescueApplication.configureAppearance();
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) throw new AssertionError("Swing scenario failed", failure.get());
    }
}
