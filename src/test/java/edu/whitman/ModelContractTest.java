package edu.whitman;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Supplementary acceptance checks for the exact worked mission in Game_Rules. */
class ModelContractTest {
    @Test
    void documentedSolutionWinsDefaultMissionInExactlyFortyActions() {
        RescueGame game = new RescueGame(new RescueTeam("Worked mission"));
        runWorkedMission(game, 40);

        assertEquals(MissionStatus.WON, game.getStatus());
        assertEquals(40, game.getTurnsRemaining());
        assertEquals(List.of("D", "A", "C", "B"),
                game.getTeam().getRescuedSurvivors().stream()
                        .map(Survivor::getId).collect(java.util.stream.Collectors.toList()));
        Medic medic = (Medic) game.getTile(5, 1).getUnit();
        assertFalse(medic.isUpgraded());
        assertEquals(0, medic.getDeliveredCount());
    }

    @Test
    void documentedSolutionWinsOnFortiethAndLastAllowedAction() {
        RescueGame game = copyDefaultMission(40);
        runWorkedMission(game, 40);

        assertEquals(MissionStatus.WON, game.getStatus());
        assertEquals(0, game.getTurnsRemaining());
    }

    @Test
    void thirtyNineActionBudgetLosesBeforeCommandCanFinish() {
        RescueGame game = copyDefaultMission(39);
        runWorkedMission(game, 39);

        assertEquals(MissionStatus.LOST, game.getStatus());
        assertEquals(0, game.getTurnsRemaining());
        assertEquals(4, game.getTeam().getRescuedCount());
        CommandUnit command = (CommandUnit) game.getTile(4, 3).getUnit();
        assertFalse(command.getTile().isShelter());
        assertThrows(IllegalStateException.class, () -> command.move(game.getTile(5, 2)));
        assertSame(command, game.getTile(4, 3).getUnit());
        assertNull(game.getTile(5, 2).getUnit());
        assertEquals(39, game.getTurnsUsed());
        assertConsistentState(game);
    }

    private RescueGame copyDefaultMission(int limit) {
        RescueGame source = new RescueGame(new RescueTeam("Source"));
        Terrain[][] terrain = new Terrain[RescueGame.HEIGHT][RescueGame.WIDTH];
        for (Tile[] row : source.getBoard()) {
            for (Tile tile : row) {
                terrain[tile.getRow()][tile.getCol()] = tile.getTerrain();
            }
        }
        RescueTeam team = new RescueTeam("Custom budget");
        RescueGame game = new RescueGame(team, terrain, limit);
        new Scout(team, game.getTile(5, 0));
        new Medic(team, game.getTile(5, 1));
        new RescueTruck(team, game.getTile(5, 3));
        new CommandUnit(team, game.getTile(5, 4));
        for (Survivor survivor : source.getSurvivors()) {
            Tile tile = survivor.getTile();
            new Survivor(survivor.getId(), game.getTile(tile.getRow(), tile.getCol()));
        }
        return game;
    }

    private void runWorkedMission(RescueGame game, int actionCount) {
        Scout scout = (Scout) game.getTile(5, 0).getUnit();
        RescueTruck truck = (RescueTruck) game.getTile(5, 3).getUnit();
        CommandUnit command = (CommandUnit) game.getTile(5, 4).getUnit();
        Survivor a = game.getTile(0, 0).getSurvivor();
        Survivor b = game.getTile(0, 4).getSurvivor();
        Survivor c = game.getTile(2, 4).getSurvivor();
        Survivor d = game.getTile(3, 0).getSurvivor();
        List<Runnable> actions = List.of(
                move(scout, 4, 0), move(scout, 3, 0), () -> scout.pickUp(d),
                move(scout, 4, 1), move(scout, 5, 2), () -> assertEquals(1, scout.deliver()),
                move(scout, 4, 1), move(scout, 3, 0), move(scout, 2, 0),
                move(scout, 1, 0), move(scout, 0, 0), () -> scout.pickUp(a),
                move(scout, 1, 0), move(scout, 2, 0), move(scout, 3, 0),
                move(scout, 4, 1), move(scout, 5, 2), () -> assertEquals(1, scout.deliver()),
                move(scout, 4, 2), move(truck, 4, 3), move(truck, 3, 3),
                move(truck, 2, 3), move(truck, 2, 4), () -> truck.pickUp(c),
                move(truck, 2, 3), move(truck, 1, 3), move(truck, 0, 3),
                move(truck, 0, 4), () -> truck.pickUp(b), move(truck, 0, 3),
                move(truck, 1, 3), move(truck, 2, 3), move(truck, 3, 3),
                move(truck, 4, 3), move(truck, 5, 3), move(truck, 5, 2),
                () -> assertEquals(2, truck.deliver()), move(truck, 5, 3),
                move(command, 4, 3), move(command, 5, 2));

        assertConsistentState(game);
        for (int i = 0; i < actionCount; i++) {
            assertEquals(MissionStatus.IN_PROGRESS, game.getStatus(), "Before action " + (i + 1));
            actions.get(i).run();
            assertEquals(i + 1, game.getTurnsUsed());
            assertEquals(Math.max(0, game.getTurnLimit() - i - 1), game.getTurnsRemaining());
            assertEquals(i >= 5, scout.isUpgraded(), "Scout upgrades during its first delivery");
            assertConsistentState(game);
            if (i == 36) {
                assertEquals(4, game.getTeam().getRescuedCount());
                assertEquals(MissionStatus.IN_PROGRESS, game.getStatus(), "Command still needs shelter");
            }
        }
    }

    private Runnable move(RescueUnit unit, int row, int col) {
        return () -> unit.move(unit.getTeam().getGame().getTile(row, col));
    }

    private void assertConsistentState(RescueGame game) {
        List<Survivor> registry = game.getSurvivors();
        List<Survivor> rescued = game.getTeam().getRescuedSurvivors();
        assertEquals(registry.size(), new HashSet<>(registry).size());
        assertEquals(rescued.size(), new HashSet<>(rescued).size());
        assertEquals(rescued.size(), game.getTeam().getRescuedCount());
        Set<Survivor> carried = new HashSet<>();
        for (RescueUnit unit : game.getTeam().getUnits()) {
            assertSame(game.getTeam(), unit.getTeam());
            assertSame(unit, unit.getTile().getUnit());
            assertSame(unit.getTile(), game.getTile(unit.getTile().getRow(), unit.getTile().getCol()));
            assertTrue(unit.getCargo().size() <= unit.getCapacity());
            for (Survivor survivor : unit.getCargo()) {
                assertTrue(carried.add(survivor), "A survivor may occur in only one cargo slot");
                assertTrue(registry.contains(survivor));
                assertSame(unit, survivor.getCarrier());
            }
        }
        for (Tile[] row : game.getBoard()) {
            for (Tile tile : row) {
                if (tile.getUnit() != null) assertSame(tile, tile.getUnit().getTile());
                if (tile.getSurvivor() != null) assertSame(tile, tile.getSurvivor().getTile());
            }
        }
        for (Survivor survivor : registry) {
            int states = (survivor.getTile() == null ? 0 : 1)
                    + (survivor.getCarrier() == null ? 0 : 1) + (survivor.isRescued() ? 1 : 0);
            assertEquals(1, states, "Each survivor has exactly one lifecycle state");
            assertEquals(survivor.isRescued(), rescued.contains(survivor));
            assertEquals(survivor.getCarrier() != null, carried.contains(survivor));
            if (survivor.getTile() != null) assertSame(survivor, survivor.getTile().getSurvivor());
        }
    }
}
