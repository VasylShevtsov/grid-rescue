package edu.whitman.ui;

import edu.whitman.CommandUnit;
import edu.whitman.Medic;
import edu.whitman.MissionStatus;
import edu.whitman.RescueGame;
import edu.whitman.RescueTruck;
import edu.whitman.RescueUnit;
import edu.whitman.Scout;
import edu.whitman.Survivor;
import edu.whitman.Tile;
import edu.whitman.app.GameController;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

import static edu.whitman.ui.MissionViewTest.click;
import static edu.whitman.ui.MissionViewTest.feedback;
import static edu.whitman.ui.MissionViewTest.find;
import static edu.whitman.ui.MissionViewTest.move;
import static edu.whitman.ui.MissionViewTest.onEventThread;
import static org.junit.jupiter.api.Assertions.*;

/** Player journeys that protect the redesigned UI's interaction and recovery contracts. */
class MissionExperienceTest {
    @Test
    void selectingUnitsOnTheMapOrWithShortcutsIsFreeAndUpdatesTheInspector() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            Class<?>[] unitTypes = {Scout.class, Medic.class, RescueTruck.class, CommandUnit.class};
            int[] columns = {0, 1, 3, 4};
            for (int i = 0; i < unitTypes.length; i++) {
                click(view, "tile-5-" + columns[i]);
                RescueUnit selected = controller.getSelectedUnit();
                assertEquals(unitTypes[i], selected.getClass());
                assertFalse(controller.isError(), feedback(view));
                assertTrue(find(view, "tile-5-" + columns[i]).getAccessibleContext()
                        .getAccessibleName().toLowerCase().contains("selected unit"));
                assertTrue(text(view, "selected-details").contains(MissionView.unitName(selected)));
                assertEquals(0, controller.getGame().getTurnsUsed());
            }
            for (int i = 0; i < unitTypes.length; i++) {
                press(view, KeyEvent.VK_1 + i);
                assertEquals(unitTypes[i], controller.getSelectedUnit().getClass());
                assertOnlyMapTabStop(view, 5, columns[i]);
                assertEquals(0, controller.getGame().getTurnsUsed());
            }
            click(view, "select-Medic");
            click(view, "tile-4-1");
            assertTrue(controller.getSelectedUnit() instanceof Medic);
            assertSame(controller.getGame().getTile(4, 1), controller.getSelectedUnit().getTile());
            assertEquals(1, controller.getGame().getTurnsUsed());
        });
    }

    @Test
    void contextualGuidanceFollowsPickupCargoDeliveryAndUpgrade() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            move(view, 4, 0, 3, 0);
            assertTrue(text(view, "context-hint").toLowerCase().contains("pick up"),
                    text(view, "context-hint"));
            assertTrue(text(view, "context-hint").contains("D"));
            assertTrue(find(view, "pickup").isEnabled());
            press(view, KeyEvent.VK_P);
            assertEquals(3, controller.getGame().getTurnsUsed());
            assertEquals("D", controller.getSelectedUnit().getCargo().get(0).getId());
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            assertTrue(text(view, "context-hint").toLowerCase().contains("shelter"),
                    text(view, "context-hint"));
            assertTrue(text(view, "survivors").contains("Aboard Scout"));
            move(view, 4, 1, 5, 2);
            assertTrue(text(view, "context-hint").toLowerCase().contains("deliver"),
                    text(view, "context-hint"));
            assertTrue(find(view, "deliver").isEnabled());
            press(view, KeyEvent.VK_D);
            assertEquals(6, controller.getGame().getTurnsUsed());
            assertEquals(1, controller.getGame().getTeam().getRescuedCount());
            assertTrue(text(view, "survivors").contains("Rescued"));
            assertTrue(text(view, "selected-details").contains("Active: water access"));
            assertTrue(feedback(view).contains("upgraded"));
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
        });
    }

    @Test
    void medicUpgradeMakesASecondPickupAvailableBeforeReturningToShelter() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            press(view, KeyEvent.VK_2);
            move(view, 4, 1, 3, 1, 3, 0);
            click(view, "pickup");
            move(view, 4, 0, 4, 1, 4, 2, 5, 2);
            click(view, "deliver");
            assertTrue(text(view, "selected-details").contains("Active: capacity 2"));
            assertEquals(2, controller.getSelectedUnit().getCapacity());
            move(view, 4, 2, 4, 3, 3, 3, 2, 3, 2, 4);
            click(view, "pickup");
            assertEquals(1, controller.getSelectedUnit().getCargo().size());
            move(view, 2, 3, 1, 3, 0, 3, 0, 4);
            assertTrue(find(view, "pickup").isEnabled());
            assertTrue(text(view, "context-hint").toLowerCase().contains("pick up"),
                    text(view, "context-hint"));
            click(view, "pickup");
            assertEquals(2, controller.getSelectedUnit().getCargo().size());
            assertTrue(text(view, "selected-details").contains("2 / 2"));
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            assertTrue(text(view, "context-hint").toLowerCase().contains("shelter"));
        });
    }

    @Test
    void helpIsReversibleAndPreventsAccidentalGameplayShortcuts() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            move(view, 4, 0, 3, 0);
            RescueUnit selected = controller.getSelectedUnit();
            Tile currentTile = selected.getTile();
            press(view, KeyEvent.VK_H);
            assertTrue(find(view, "help-panel").isVisible());
            press(view, KeyEvent.VK_2);
            press(view, KeyEvent.VK_P);
            press(view, KeyEvent.VK_D);
            assertSame(selected, controller.getSelectedUnit());
            assertSame(currentTile, selected.getTile());
            assertTrue(selected.getCargo().isEmpty());
            assertEquals(2, controller.getGame().getTurnsUsed());
            press(view, KeyEvent.VK_ESCAPE);
            assertFalse(find(view, "help-panel").isVisible());
            click(view, "help");
            assertTrue(find(view, "help-panel").isVisible());
            click(view, "close-help");
            assertFalse(find(view, "help-panel").isVisible());
            press(view, KeyEvent.VK_P);
            assertEquals(1, selected.getCargo().size());
            assertEquals(3, controller.getGame().getTurnsUsed());
        });
    }

    @Test
    void restartProtectsProgressAndCancelOrEscapeKeepsTheExactMission() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            move(view, 4, 0, 3, 0);
            click(view, "pickup");
            RescueGame original = controller.getGame();
            RescueUnit scout = controller.getSelectedUnit();
            Survivor survivor = scout.getCargo().get(0);
            click(view, "restart");
            assertTrue(find(view, "restart-panel").isVisible());
            assertSame(original, controller.getGame());
            press(view, KeyEvent.VK_2);
            press(view, KeyEvent.VK_P);
            press(view, KeyEvent.VK_D);
            assertSame(scout, controller.getSelectedUnit());
            assertEquals(3, original.getTurnsUsed());
            click(view, "cancel-restart");
            assertFalse(find(view, "restart-panel").isVisible());
            assertSame(original, controller.getGame());
            assertSame(survivor, scout.getCargo().get(0));
            assertSame(scout, survivor.getCarrier());
            click(view, "restart");
            press(view, KeyEvent.VK_ESCAPE);
            assertFalse(find(view, "restart-panel").isVisible());
            assertSame(original, controller.getGame());
            click(view, "restart");
            click(view, "confirm-restart");
            RescueGame fresh = controller.getGame();
            assertNotSame(original, fresh);
            assertFalse(find(view, "restart-panel").isVisible());
            assertEquals(0, fresh.getTurnsUsed());
            assertEquals(80, fresh.getTurnsRemaining());
            assertEquals(MissionStatus.IN_PROGRESS, fresh.getStatus());
            assertTrue(controller.getSelectedUnit() instanceof Scout);
            assertNotSame(scout, controller.getSelectedUnit());
            assertTrue(controller.getSelectedUnit().getCargo().isEmpty());
            for (int i = 0; i < fresh.getSurvivors().size(); i++) {
                assertNotSame(original.getSurvivors().get(i), fresh.getSurvivors().get(i));
            }
            assertEquals(3, original.getTurnsUsed(), "Restart must not mutate the discarded mission");
            assertSame(survivor, scout.getCargo().get(0));
        });
    }

    @Test
    void aFreshMissionRestartsWithoutAnUnnecessaryConfirmation() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            RescueGame original = controller.getGame();
            click(view, "restart");
            assertNotSame(original, controller.getGame());
            assertFalse(find(view, "restart-panel").isVisible());
            assertEquals(0, controller.getGame().getTurnsUsed());
        });
    }

    @Test
    void enterActivatesTheFocusedRosterHelpAndRestartControls() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            enter(view, "select-Medic");
            assertTrue(controller.getSelectedUnit() instanceof Medic);
            assertEquals(0, controller.getGame().getTurnsUsed());
            enter(view, "help");
            assertTrue(find(view, "help-panel").isVisible());
            enter(view, "close-help");
            assertFalse(find(view, "help-panel").isVisible());
            move(view, 4, 1);
            RescueGame original = controller.getGame();
            enter(view, "restart");
            assertTrue(find(view, "restart-panel").isVisible());
            enter(view, "cancel-restart");
            assertFalse(find(view, "restart-panel").isVisible());
            assertSame(original, controller.getGame());
            assertEquals(1, original.getTurnsUsed());
            enter(view, "restart");
            enter(view, "confirm-restart");
            assertNotSame(original, controller.getGame());
            assertEquals(0, controller.getGame().getTurnsUsed());
        });
    }

    @Test
    void arrowNavigationDoesNotSpendActionsAndEnterActivatesTheFocusedTile() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            JComponent initial = (JComponent) find(view, "tile-5-0");
            assertOnlyMapTabStop(view, 5, 0);
            invokeBinding(initial, JComponent.WHEN_FOCUSED, KeyEvent.VK_UP);
            assertOnlyMapTabStop(view, 4, 0);
            invokeBinding((JComponent) find(view, "tile-4-0"), JComponent.WHEN_FOCUSED, KeyEvent.VK_DOWN);
            assertOnlyMapTabStop(view, 5, 0);
            invokeBinding(initial, JComponent.WHEN_FOCUSED, KeyEvent.VK_LEFT);
            assertOnlyMapTabStop(view, 5, 0);
            invokeBinding(initial, JComponent.WHEN_FOCUSED, KeyEvent.VK_RIGHT);
            assertOnlyMapTabStop(view, 5, 1);
            assertEquals(0, controller.getGame().getTurnsUsed());
            assertSame(controller.getGame().getTile(5, 0), controller.getSelectedUnit().getTile());
            JComponent destination = (JComponent) find(view, "tile-4-0");
            invokeBinding(destination, JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER);
            assertOnlyMapTabStop(view, 4, 0);
            assertEquals(1, controller.getGame().getTurnsUsed());
            assertSame(controller.getGame().getTile(4, 0), controller.getSelectedUnit().getTile());
            assertFalse(controller.isError(), feedback(view));
        });
    }

    @Test
    void victoryGivesAResultAndReplayWhileKeepingUnitsInspectable() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            MissionViewTest.firstRescue(view);
            MissionViewTest.finishMission(view);
            RescueGame completed = controller.getGame();
            assertEquals(MissionStatus.WON, completed.getStatus());
            assertEquals(40, completed.getTurnsUsed());
            assertTrue(find(view, "result-panel").isVisible());
            assertFalse(text(view, "result-title").isBlank());
            assertTrue(find(view, "play-again").isEnabled());
            assertFalse(find(view, "tile-0-0").isEnabled());
            assertFalse(find(view, "pickup").isEnabled());
            assertFalse(find(view, "deliver").isEnabled());
            assertTrue(find(view, "tile-4-2").isEnabled());
            click(view, "tile-4-2");
            assertTrue(controller.getSelectedUnit() instanceof Scout);
            assertEquals(40, completed.getTurnsUsed());
            assertTrue(find(view, "result-panel").isVisible());
            click(view, "play-again");
            assertNotSame(completed, controller.getGame());
            assertEquals(MissionStatus.IN_PROGRESS, controller.getGame().getStatus());
            assertEquals(0, controller.getGame().getTurnsUsed());
            assertEquals(0, controller.getGame().getTeam().getRescuedCount());
            assertFalse(find(view, "result-panel").isVisible());
            assertFalse(find(view, "restart-panel").isVisible());
        });
    }

    @Test
    void exhaustedBudgetHasAResultAndReplayWithoutASecondConfirmation() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController(MissionViewTest.shortMission());
            MissionView view = new MissionView(controller);
            RescueGame completed = controller.getGame();
            click(view, "tile-0-1");
            assertEquals(MissionStatus.LOST, completed.getStatus());
            assertEquals(0, completed.getTurnsRemaining());
            assertTrue(find(view, "result-panel").isVisible());
            assertFalse(text(view, "result-title").isBlank());
            assertTrue(feedback(view).contains("Mission lost"));
            press(view, KeyEvent.VK_P);
            press(view, KeyEvent.VK_D);
            assertEquals(1, completed.getTurnsUsed());
            click(view, "play-again");
            assertNotSame(completed, controller.getGame());
            assertEquals(80, controller.getGame().getTurnsRemaining());
            assertFalse(find(view, "restart-panel").isVisible());
            assertFalse(find(view, "result-panel").isVisible());
        });
    }

    @Test
    void everyMapTileExposesAReadableLocationAndTerrainBeforeAndAfterMovement() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            for (int row = 0; row < RescueGame.HEIGHT; row++) {
                for (int col = 0; col < RescueGame.WIDTH; col++) {
                    Component tile = find(view, "tile-" + row + "-" + col);
                    String accessible = tile.getAccessibleContext().getAccessibleName();
                    assertNotNull(accessible);
                    assertTrue(accessible.contains("Row " + row + ", column " + col), accessible);
                    assertTrue(accessible.contains(TileButton.terrainLabel(controller.getGame()
                            .getTile(row, col).getTerrain())), accessible);
                    assertNotNull(((JButton) tile).getToolTipText());
                }
            }
            move(view, 4, 0, 3, 0);
            assertTrue(find(view, "tile-3-0").getAccessibleContext().getAccessibleName().contains("survivor D"));
            click(view, "pickup");
            assertFalse(find(view, "tile-3-0").getAccessibleContext().getAccessibleName().contains("survivor D"));
            assertTrue(find(view, "tile-3-0").getAccessibleContext().getAccessibleName().toLowerCase().contains("selected unit"));
        });
    }

    @Test
    void compactHelpKeepsTheCompleteRulesReadable() throws Exception {
        onEventThread(() -> {
            MissionView view = new MissionView(new GameController());
            JScrollPane window = GridRescueApplication.scrollableMission(view);
            window.setSize(980, 760);
            click(view, "help");
            layoutForPaint(window);
            layoutForPaint(window);
            JLabel rules = labelContaining((Container) find(view, "help-panel"), "Every action matters.");
            assertNotNull(rules);
            assertTrue(rules.getHeight() >= rules.getPreferredSize().height,
                    "All help rules must fit or be reachable by scrolling: " + rules.getHeight()
                            + " allocated for " + rules.getPreferredSize().height + " pixels of content");
            assertTrue(rules.getWidth() >= rules.getPreferredSize().width,
                    "Rules must not be cut off horizontally");
        });
    }

    @Test
    void compactMissionKeepsTilesAndEssentialGuidanceReadable() throws Exception {
        onEventThread(() -> {
            MissionView view = new MissionView(new GameController());
            view.setSize(980, 760);
            layoutForPaint(view);
            assertTrue(find(view, "tile-5-0").getWidth() >= 64);
            assertTrue(find(view, "tile-5-0").getHeight() >= 64);
            for (String name : new String[] {"selected-details", "context-hint", "survivors"}) {
                Component component = find(view, name);
                assertTrue(component.getHeight() >= component.getPreferredSize().height,
                        name + " must remain readable in a compact window");
            }
            assertTrue(find(view, "pickup").getHeight() >= 36);
            assertTrue(find(view, "deliver").getHeight() >= 36);
        });
    }

    @Test
    void resultAndRestartMessagesHaveRoomAfterTheMissionHasAlreadyBeenLaidOut() throws Exception {
        onEventThread(() -> {
            GameController controller = new GameController();
            MissionView view = new MissionView(controller);
            view.setSize(1160, 820);
            layoutForPaint(view);
            move(view, 4, 0, 3, 0);
            click(view, "pickup");
            click(view, "restart");
            layoutForPaint(view);
            JLabel summary = labelContaining((Container) find(view, "restart-panel"), "3 actions played");
            assertNotNull(summary);
            assertTrue(summary.getHeight() >= summary.getPreferredSize().height);
            assertTrue(summary.getWidth() >= summary.getPreferredSize().width);
            click(view, "cancel-restart");
            move(view, 4, 1, 5, 2);
            click(view, "deliver");
            MissionViewTest.finishMission(view);
            layoutForPaint(view);
            JLabel title = (JLabel) find(view, "result-title");
            assertTrue(title.getHeight() >= title.getPreferredSize().height,
                    "The victory message must be visible after an active mission transitions to won");
            assertTrue(title.getWidth() >= title.getPreferredSize().width);
        });
    }

    private static String text(MissionView view, String name) {
        return ((JLabel) find(view, name)).getText();
    }

    private static void assertOnlyMapTabStop(MissionView view, int focusRow, int focusCol) {
        for (int row = 0; row < RescueGame.HEIGHT; row++) {
            for (int col = 0; col < RescueGame.WIDTH; col++) {
                String name = "tile-" + row + "-" + col;
                assertEquals(row == focusRow && col == focusCol, find(view, name).isFocusable(), name);
            }
        }
    }

    private static JLabel labelContaining(Container parent, String content) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getText() != null
                    && ((JLabel) child).getText().contains(content)) return (JLabel) child;
            if (child instanceof Container) {
                JLabel found = labelContaining((Container) child, content);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layoutForPaint(Container parent) {
        invalidateTree(parent);
        layoutTree(parent);
    }

    private static void invalidateTree(Container parent) {
        parent.invalidate();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) invalidateTree((Container) child);
        }
    }

    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) layoutTree((Container) child);
        }
    }

    private static void press(MissionView view, int key) {
        invokeBinding(view, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, key);
    }

    private static void enter(MissionView view, String control) {
        invokeBinding((JComponent) find(view, control), JComponent.WHEN_FOCUSED, KeyEvent.VK_ENTER);
    }

    private static void invokeBinding(JComponent component, int condition, int key) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, 0);
        Object binding = component.getInputMap(condition).get(stroke);
        assertNotNull(binding, "Missing keyboard shortcut: " + KeyEvent.getKeyText(key));
        Action action = component.getActionMap().get(binding);
        assertNotNull(action, "Missing keyboard action: " + binding);
        action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, binding.toString()));
    }
}
