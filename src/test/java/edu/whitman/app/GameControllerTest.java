package edu.whitman.app;

import edu.whitman.CommandUnit;
import edu.whitman.Medic;
import edu.whitman.MissionStatus;
import edu.whitman.RescueGame;
import edu.whitman.RescueTeam;
import edu.whitman.RescueTruck;
import edu.whitman.RescueUnit;
import edu.whitman.Scout;
import edu.whitman.Survivor;
import edu.whitman.Terrain;
import edu.whitman.Tile;
import edu.whitman.UpgradeableUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Supplementary interface tests; the accepted model tests remain unchanged. */
class GameControllerTest {
    @Test
    void startsWithTheDefaultMissionAndScoutSelected() {
        GameController controller = new GameController();
        RescueGame game = controller.getGame();
        assertEquals("Grid Rescue", game.getTeam().getName());
        assertSame(game.getTile(5, 0).getUnit(), controller.getSelectedUnit());
        assertEquals(4, game.getTeam().getUnits().size());
        assertEquals(4, game.getSurvivors().size());
        assertEquals(80, game.getTurnsRemaining());
        assertFalse(controller.isError());
        assertTrue(controller.getMessage().contains("shelter"));
        assertThrows(IllegalArgumentException.class, () -> new GameController(null));
    }

    @Test
    void selectionCostsNoActionAndRejectsNullOrForeignUnits() {
        GameController controller = new GameController();
        RescueGame game = controller.getGame();
        RescueUnit medic = game.getTile(5, 1).getUnit();
        String before = snapshot(game);
        controller.selectUnit(medic);
        assertSame(medic, controller.getSelectedUnit());
        assertEquals(before, snapshot(game));
        assertFalse(controller.isError());
        assertRejected(controller, () -> controller.selectUnit(null));
        assertSame(medic, controller.getSelectedUnit());
        RescueUnit foreign = new GameController().getSelectedUnit();
        assertRejected(controller, () -> controller.selectUnit(foreign));
        assertSame(medic, controller.getSelectedUnit());
    }

    @Test
    void consecutiveMovesUseOneActionEachAndAValidActionClearsAnError() {
        GameController controller = new GameController();
        assertRejected(controller, () -> controller.moveTo(5, 1));
        assertTrue(controller.getMessage().contains("occupied"));
        moveAlong(controller, 4, 0, 3, 0);
        assertEquals(2, controller.getGame().getTurnsUsed());
        assertSame(controller.getGame().getTile(3, 0), controller.getSelectedUnit().getTile());
        assertTrue(controller.getSelectedUnit().getCargo().isEmpty());
        assertNotNull(controller.getGame().getTile(3, 0).getSurvivor(),
                "Movement onto a survivor must not automatically pick it up");
        assertFalse(controller.isError());
    }

    @Test
    void invalidMovesPreserveAllStateAndGiveContext() {
        Terrain[][] map = terrain();
        map[0][0] = Terrain.BLOCKED;
        map[1][1] = Terrain.WATER;
        RescueGame game = customGame(map, 80);
        new Scout(game.getTeam(), game.getTile(1, 0));
        new Medic(game.getTeam(), game.getTile(2, 0));
        GameController controller = new GameController(game);
        assertRejected(controller, () -> controller.moveTo(1, 0));
        assertTrue(controller.getMessage().contains("current tile"));
        assertRejected(controller, () -> controller.moveTo(2, 0));
        assertTrue(controller.getMessage().contains("occupied"));
        assertRejected(controller, () -> controller.moveTo(0, 0));
        assertTrue(controller.getMessage().contains("blocked"));
        assertRejected(controller, () -> controller.moveTo(1, 1));
        assertTrue(controller.getMessage().contains("water"));
        assertRejected(controller, () -> controller.moveTo(5, 4));
        assertTrue(controller.getMessage().contains("neighboring"));
        for (int[] point : new int[][]{{-1, 0}, {6, 0}, {0, -1}, {0, 5}}) {
            assertRejected(controller, () -> controller.moveTo(point[0], point[1]));
            assertTrue(controller.getMessage().contains("board limits"));
        }
    }

    @Test
    void pickupPreservesCargoAcrossSelectionAndRejectsMissingOrExcessCargo() {
        RescueGame game = customGame(terrain(), 80);
        Scout scout = new Scout(game.getTeam(), game.getTile(0, 0));
        Medic medic = new Medic(game.getTeam(), game.getTile(4, 0));
        Survivor first = new Survivor("first", game.getTile(0, 0));
        Survivor second = new Survivor("second", game.getTile(0, 1));
        GameController controller = new GameController(game);
        successful(controller, controller::pickUp);
        assertEquals(List.of(first), scout.getCargo());
        assertSame(scout, first.getCarrier());
        assertNull(first.getTile());
        assertNull(scout.getTile().getSurvivor());
        assertEquals(1, game.getTurnsUsed());
        controller.selectUnit(medic);
        assertRejected(controller, controller::pickUp);
        assertTrue(controller.getMessage().contains("no waiting survivor"));
        controller.selectUnit(scout);
        moveAlong(controller, 0, 1);
        assertRejected(controller, controller::pickUp);
        assertTrue(controller.getMessage().contains("full"));
        assertEquals(List.of(first), scout.getCargo());
        assertSame(game.getTile(0, 1), second.getTile());
    }

    @Test
    void commandUnitCannotPickUpASurvivor() {
        RescueGame game = customGame(terrain(), 80);
        new CommandUnit(game.getTeam(), game.getTile(0, 0));
        new Survivor("A", game.getTile(0, 0));
        GameController controller = new GameController(game);
        assertRejected(controller, controller::pickUp);
        assertTrue(controller.getMessage().contains("cannot carry"));
    }

    @Test
    void deliveryRequiresShelterAndCargo() {
        GameController controller = new GameController();
        assertRejected(controller, controller::deliver);
        assertTrue(controller.getMessage().contains("shelter"));
        moveAlong(controller, 4, 1, 5, 2);
        assertRejected(controller, controller::deliver);
        assertTrue(controller.getMessage().contains("empty"));
    }

    @Test
    void scoutDeliveryReportsWaterUpgradeOnceAndTracksRescuedState() {
        Terrain[][] map = terrain();
        map[1][1] = Terrain.SHELTER;
        map[1][2] = Terrain.WATER;
        RescueGame game = customGame(map, 80);
        Scout scout = new Scout(game.getTeam(), game.getTile(1, 1));
        Survivor first = new Survivor("A", game.getTile(1, 1));
        Survivor second = new Survivor("B", game.getTile(1, 2));
        GameController controller = new GameController(game);
        successful(controller, controller::pickUp);
        successful(controller, controller::deliver);
        assertTrue(controller.getMessage().contains("upgraded"));
        assertTrue(controller.getMessage().contains("water"));
        assertTrue(scout.isUpgraded());
        assertTrue(first.isRescued());
        assertNull(first.getTile());
        assertNull(first.getCarrier());
        assertEquals(1, game.getTeam().getRescuedCount());
        assertEquals(1, scout.getDeliveredCount());
        assertTrue(scout.getCargo().isEmpty());
        moveAlong(controller, 1, 2);
        successful(controller, controller::pickUp);
        moveAlong(controller, 1, 1);
        successful(controller, controller::deliver);
        assertFalse(controller.getMessage().contains("upgraded"), "Only new upgrades should be announced");
        assertTrue(controller.getMessage().contains("Command Unit"));
        assertEquals(List.of(first, second), game.getTeam().getRescuedSurvivors());
        assertEquals(6, game.getTurnsUsed());
        assertEquals(MissionStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void medicUpgradeAllowsTwoSurvivorsAndBatchDeliveryStillCostsOneAction() {
        Terrain[][] map = terrain();
        map[1][1] = Terrain.SHELTER;
        RescueGame game = customGame(map, 80);
        Medic medic = new Medic(game.getTeam(), game.getTile(1, 1));
        new Survivor("A", game.getTile(1, 1));
        Survivor second = new Survivor("B", game.getTile(1, 2));
        Survivor third = new Survivor("C", game.getTile(1, 3));
        GameController controller = new GameController(game);
        successful(controller, controller::pickUp);
        successful(controller, controller::deliver);
        assertTrue(controller.getMessage().contains("carry two"));
        assertTrue(medic.isUpgraded());
        assertEquals(2, medic.getCapacity());
        moveAlong(controller, 1, 2);
        successful(controller, controller::pickUp);
        moveAlong(controller, 1, 3);
        successful(controller, controller::pickUp);
        assertEquals(List.of(second, third), medic.getCargo());
        moveAlong(controller, 1, 2, 1, 1);
        int beforeDelivery = game.getTurnsUsed();
        successful(controller, controller::deliver);
        assertEquals(beforeDelivery + 1, game.getTurnsUsed());
        assertEquals(3, game.getTeam().getRescuedCount());
        assertEquals(3, medic.getDeliveredCount());
        assertTrue(controller.getMessage().contains("delivered 2 survivors"));
        assertFalse(controller.getMessage().contains("upgraded"));
    }

    @Test
    void restartReplacesTheEntireMissionWithoutMutatingTheOldOne() {
        GameController controller = new GameController();
        moveAlong(controller, 4, 0, 3, 0);
        successful(controller, controller::pickUp);
        RescueGame oldGame = controller.getGame();
        RescueUnit oldUnit = controller.getSelectedUnit();
        String oldState = snapshot(oldGame);
        controller.restart();
        RescueGame freshGame = controller.getGame();
        assertNotSame(oldGame, freshGame);
        assertNotSame(oldGame.getTeam(), freshGame.getTeam());
        assertNotSame(oldUnit, controller.getSelectedUnit());
        assertEquals(oldState, snapshot(oldGame));
        assertSame(freshGame.getTile(5, 0).getUnit(), controller.getSelectedUnit());
        assertEquals(0, freshGame.getTurnsUsed());
        assertEquals(80, freshGame.getTurnsRemaining());
        assertEquals(0, freshGame.getTeam().getRescuedCount());
        assertFalse(((Scout) controller.getSelectedUnit()).isUpgraded());
        assertNotNull(freshGame.getTile(3, 0).getSurvivor());
        assertFalse(controller.isError());
        assertRejected(controller, () -> controller.selectUnit(oldUnit));
    }

    @Test
    void finalActionDeliveryWinsAndTerminalActionsCannotChangeState() {
        Terrain[][] map = terrain();
        map[1][1] = Terrain.SHELTER;
        RescueGame game = customGame(map, 2);
        Medic medic = new Medic(game.getTeam(), game.getTile(1, 1));
        CommandUnit command = new CommandUnit(game.getTeam(), game.getTile(5, 2));
        new Survivor("A", game.getTile(1, 1));
        GameController controller = new GameController(game);
        successful(controller, controller::pickUp);
        successful(controller, controller::deliver);
        assertEquals(MissionStatus.WON, game.getStatus());
        assertEquals(0, game.getTurnsRemaining());
        assertTrue(medic.isUpgraded());
        assertTrue(controller.getMessage().contains("upgraded"));
        assertTrue(controller.getMessage().contains("won"));
        assertTerminalActions(controller, "won");
        controller.selectUnit(command);
        assertSame(command, controller.getSelectedUnit());
        assertFalse(controller.isError());
        assertTrue(controller.getMessage().contains("won"));
        assertEquals(2, game.getTurnsUsed());
    }

    @Test
    void lastActionReportsLossAndRestartClearsTerminalState() {
        RescueGame game = customGame(terrain(), 1);
        new Scout(game.getTeam(), game.getTile(0, 0));
        GameController controller = new GameController(game);
        successful(controller, () -> controller.moveTo(0, 1));
        assertEquals(MissionStatus.LOST, game.getStatus());
        assertTrue(controller.getMessage().contains("lost"));
        assertTerminalActions(controller, "lost");
        controller.restart();
        assertEquals(MissionStatus.IN_PROGRESS, controller.getGame().getStatus());
        assertEquals(4, controller.getGame().getTeam().getUnits().size());
        assertEquals(80, controller.getGame().getTurnLimit());
        assertFalse(controller.isError());
    }

    @Test
    void emptyCustomMissionHasHelpfulFeedbackAndCanRestart() {
        RescueGame game = customGame(terrain(), 10);
        GameController controller = new GameController(game);
        assertNull(controller.getSelectedUnit());
        assertTrue(controller.getMessage().contains("no units"));
        assertRejected(controller, () -> controller.moveTo(0, 0));
        assertRejected(controller, controller::pickUp);
        assertRejected(controller, controller::deliver);
        assertTrue(controller.getMessage().contains("Select a unit"));
        controller.restart();
        assertNotNull(controller.getSelectedUnit());
        assertEquals(4, controller.getGame().getSurvivors().size());
        assertEquals(80, controller.getGame().getTurnLimit());
    }

    @Test
    void exactWorkedSolutionWinsTheDefaultMissionInFortyControllerActions() {
        GameController controller = new GameController();
        runWorkedSolutionThroughAction39(controller);
        successful(controller, () -> controller.moveTo(5, 2));
        assertCompletedWorkedSolution(controller);
        assertEquals(40, controller.getGame().getTurnsRemaining());
    }

    @Test
    void exactWorkedSolutionWinsOnItsFortiethAndFinalAllowedAction() {
        GameController controller = new GameController(copyDefaultMission(40));
        runWorkedSolutionThroughAction39(controller);
        successful(controller, () -> controller.moveTo(5, 2));
        assertCompletedWorkedSolution(controller);
        assertEquals(0, controller.getGame().getTurnsRemaining());
    }

    @Test
    void workedSolutionWithOnlyThirtyNineActionsLosesBeforeCommandArrives() {
        GameController controller = new GameController(copyDefaultMission(39));
        runWorkedSolutionThroughAction39(controller);
        assertEquals(MissionStatus.LOST, controller.getGame().getStatus());
        assertEquals(4, controller.getGame().getTeam().getRescuedCount());
        assertRejected(controller, () -> controller.moveTo(5, 2));
        assertTrue(controller.getMessage().contains("lost"));
        assertSame(controller.getGame().getTile(4, 3), controller.getSelectedUnit().getTile());
    }

    /** Exactly actions 1-39 in Game_Rules.docx, including both truck pickups. */
    private static void runWorkedSolutionThroughAction39(GameController controller) {
        RescueGame game = controller.getGame();
        Scout scout = (Scout) game.getTile(5, 0).getUnit();
        RescueUnit truck = game.getTile(5, 3).getUnit();
        RescueUnit command = game.getTile(5, 4).getUnit();
        controller.selectUnit(scout);
        moveAlong(controller, 4, 0, 3, 0);
        successful(controller, controller::pickUp);
        moveAlong(controller, 4, 1, 5, 2);
        successful(controller, controller::deliver);
        assertTrue(scout.isUpgraded());
        moveAlong(controller, 4, 1, 3, 0, 2, 0, 1, 0, 0, 0);
        successful(controller, controller::pickUp);
        moveAlong(controller, 1, 0, 2, 0, 3, 0, 4, 1, 5, 2);
        successful(controller, controller::deliver);
        moveAlong(controller, 4, 2);
        controller.selectUnit(truck);
        moveAlong(controller, 4, 3, 3, 3, 2, 3, 2, 4);
        successful(controller, controller::pickUp);
        moveAlong(controller, 2, 3, 1, 3, 0, 3, 0, 4);
        successful(controller, controller::pickUp);
        assertEquals(2, truck.getCargo().size());
        moveAlong(controller, 0, 3, 1, 3, 2, 3, 3, 3, 4, 3, 5, 3, 5, 2);
        successful(controller, controller::deliver);
        assertEquals(37, game.getTurnsUsed());
        assertEquals(4, game.getTeam().getRescuedCount());
        assertEquals(MissionStatus.IN_PROGRESS, game.getStatus());
        moveAlong(controller, 5, 3);
        controller.selectUnit(command);
        moveAlong(controller, 4, 3);
        assertEquals(39, game.getTurnsUsed());
    }

    private static void assertCompletedWorkedSolution(GameController controller) {
        RescueGame game = controller.getGame();
        assertEquals(40, game.getTurnsUsed());
        assertEquals(MissionStatus.WON, game.getStatus());
        assertEquals(4, game.getTeam().getRescuedCount());
        for (Survivor survivor : game.getSurvivors()) {
            assertTrue(survivor.isRescued());
            assertNull(survivor.getTile());
            assertNull(survivor.getCarrier());
        }
        assertTrue(controller.getMessage().contains("won"));
    }

    private static void assertTerminalActions(GameController controller, String outcome) {
        assertTrue(controller.getSelectedUnit().getLegalMoves().isEmpty());
        assertRejected(controller, () -> controller.moveTo(0, 0));
        assertTrue(controller.getMessage().contains(outcome));
        assertRejected(controller, () -> controller.moveTo(-1, 999));
        assertTrue(controller.getMessage().contains(outcome), "Terminal feedback precedes bad coordinates");
        assertRejected(controller, controller::pickUp);
        assertTrue(controller.getMessage().contains(outcome));
        assertRejected(controller, controller::deliver);
        assertTrue(controller.getMessage().contains(outcome));
    }

    private static void moveAlong(GameController controller, int... coordinates) {
        for (int i = 0; i < coordinates.length; i += 2) {
            int row = coordinates[i];
            int col = coordinates[i + 1];
            successful(controller, () -> controller.moveTo(row, col));
        }
    }

    private static void successful(GameController controller, Runnable action) {
        int before = controller.getGame().getTurnsUsed();
        assertDoesNotThrow(action::run);
        assertFalse(controller.isError(), controller.getMessage());
        assertEquals(before + 1, controller.getGame().getTurnsUsed());
    }

    private static void assertRejected(GameController controller, Runnable action) {
        String before = snapshot(controller.getGame());
        RescueUnit selected = controller.getSelectedUnit();
        assertDoesNotThrow(action::run);
        assertTrue(controller.isError(), controller.getMessage());
        assertFalse(controller.getMessage().isBlank());
        assertEquals(before, snapshot(controller.getGame()), "Rejected input must preserve model state");
        assertSame(selected, controller.getSelectedUnit());
    }

    private static Terrain[][] terrain() {
        Terrain[][] map = new Terrain[RescueGame.HEIGHT][RescueGame.WIDTH];
        for (Terrain[] row : map) {
            Arrays.fill(row, Terrain.PLAIN);
        }
        map[5][2] = Terrain.SHELTER;
        return map;
    }

    private static RescueGame customGame(Terrain[][] terrain, int limit) {
        return new RescueGame(new RescueTeam("Controller test"), terrain, limit);
    }

    private static RescueGame copyDefaultMission(int limit) {
        RescueGame original = new RescueGame(new RescueTeam("Template"));
        Terrain[][] map = terrain();
        for (Tile[] row : original.getBoard()) {
            for (Tile tile : row) {
                map[tile.getRow()][tile.getCol()] = tile.getTerrain();
            }
        }
        RescueGame game = customGame(map, limit);
        RescueTeam team = game.getTeam();
        new Scout(team, game.getTile(5, 0));
        new Medic(team, game.getTile(5, 1));
        new RescueTruck(team, game.getTile(5, 3));
        new CommandUnit(team, game.getTile(5, 4));
        for (Survivor survivor : original.getSurvivors()) {
            Tile tile = survivor.getTile();
            new Survivor(survivor.getId(), game.getTile(tile.getRow(), tile.getCol()));
        }
        return game;
    }

    /** Captures public state to check action atomicity without model internals. */
    private static String snapshot(RescueGame game) {
        StringBuilder state = new StringBuilder();
        state.append(game.getTeam().getName()).append(game.getStatus())
                .append(game.getTurnsUsed()).append(game.getTurnsRemaining())
                .append(game.getTeam().getRescuedSurvivors());
        for (Tile[] row : game.getBoard()) {
            for (Tile tile : row) {
                state.append(tile).append(tile.getTerrain()).append(tile.getUnit()).append(tile.getSurvivor());
            }
        }
        for (RescueUnit unit : game.getTeam().getUnits()) {
            state.append(unit).append(unit.getTile()).append(unit.getCargo())
                    .append(unit.getCapacity()).append(unit.getDeliveredCount());
            if (unit instanceof UpgradeableUnit) {
                state.append(((UpgradeableUnit) unit).isUpgraded());
            }
        }
        for (Survivor survivor : game.getSurvivors()) {
            state.append(survivor).append(survivor.getId()).append(survivor.getTile())
                    .append(survivor.getCarrier()).append(survivor.isRescued());
        }
        return state.toString();
    }
}
