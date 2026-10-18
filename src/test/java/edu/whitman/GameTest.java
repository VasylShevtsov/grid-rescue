package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class GameTest {
    @Test
    void t01_defaultMissionHasExactTerrainUnitsAndSurvivors() {
        RescueTeam team = new RescueTeam("Default");
        RescueGame game = new RescueGame(team);
        Terrain P = Terrain.PLAIN, W = Terrain.WATER, R = Terrain.RUBBLE;
        Terrain B = Terrain.BLOCKED, H = Terrain.SHELTER;
        Terrain[][] expected = {{P,P,P,P,P},{P,W,W,P,R},{R,W,B,P,P},
                                {P,P,B,P,P},{P,P,P,P,P},{P,P,H,P,P}};
        assertEquals(6, RescueGame.HEIGHT);
        assertEquals(5, RescueGame.WIDTH);
        assertEquals(80, RescueGame.DEFAULT_TURN_LIMIT);
        for (int row = 0; row < 6; row++) for (int col = 0; col < 5; col++) {
            assertEquals(expected[row][col], game.getTile(row, col).getTerrain());
        }
        assertEquals(4, team.getUnits().size());
        assertTrue(game.getTile(5,0).getUnit() instanceof Scout);
        assertTrue(game.getTile(5,1).getUnit() instanceof Medic);
        assertTrue(game.getTile(5,3).getUnit() instanceof RescueTruck);
        assertTrue(game.getTile(5,4).getUnit() instanceof CommandUnit);
        assertEquals(4, game.getSurvivors().size());
        String[] ids = {"A", "B", "C", "D"};
        int[][] positions = {{0,0},{0,4},{2,4},{3,0}};
        for (int i = 0; i < ids.length; i++) {
            Survivor survivor = game.getTile(positions[i][0],positions[i][1]).getSurvivor();
            assertNotNull(survivor);
            assertEquals(ids[i], survivor.getId());
            assertWaiting(survivor, game.getTile(positions[i][0],positions[i][1]));
        }
        assertSame(team, game.getTeam());
        assertSame(game, team.getGame());
        assertEquals(80, game.getTurnLimit());
        assertEquals(80, game.getTurnsRemaining());
        assertEquals(0, game.getTurnsUsed());
        assertEquals(MissionStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void t02_customSetupIsEmptyAndCopiesCallerTerrain() {
        Terrain[][] source = terrain(0,0,5,4);
        source[2][3] = Terrain.RUBBLE;
        Fixture f = fixture(source, 17);
        source[2][3] = Terrain.WATER;
        source[0] = new Terrain[5];
        assertEquals(Terrain.RUBBLE, f.tile(2,3).getTerrain());
        assertEquals(Terrain.SHELTER, f.tile(0,0).getTerrain());
        assertTrue(f.team.getUnits().isEmpty());
        assertTrue(f.game.getSurvivors().isEmpty());
        for (Tile[] row : f.game.getBoard()) for (Tile tile : row) {
            assertNull(tile.getUnit());
            assertNull(tile.getSurvivor());
        }
        assertEquals(17, f.game.getTurnsRemaining());
        assertEquals(0, f.game.getTurnsUsed());
        assertEquals(MissionStatus.IN_PROGRESS, f.game.getStatus());
    }

    @Test
    void t03_invalidGameConstructionDoesNotBindTeam() {
        Terrain[][] noShelter = terrain();
        Terrain[][] ragged = terrain(5,2); ragged[2] = new Terrain[4];
        Terrain[][] nullRow = terrain(5,2); nullRow[1] = null;
        Terrain[][] nullEntry = terrain(5,2); nullEntry[1][1] = null;
        Terrain[][][] badMaps = {null, new Terrain[5][5], new Terrain[6][6],
                                ragged, nullRow, nullEntry, noShelter};
        for (Terrain[][] map : badMaps) {
            RescueTeam team = new RescueTeam("Unbound");
            assertThrows(IllegalArgumentException.class, () -> new RescueGame(team, map, 80));
            assertNull(team.getGame());
            assertTrue(team.getUnits().isEmpty());
            assertTrue(team.getRescuedSurvivors().isEmpty());
            assertDoesNotThrow(() -> new RescueGame(team, terrain(5,2), 1));
        }
        for (int limit : new int[]{0,-1,Integer.MIN_VALUE}) {
            RescueTeam team = new RescueTeam("Limit");
            assertThrows(IllegalArgumentException.class, () -> new RescueGame(team, terrain(5,2), limit));
            assertNull(team.getGame());
        }
        assertThrows(IllegalArgumentException.class, () -> new RescueGame(null));
        assertThrows(IllegalArgumentException.class, () -> new RescueGame(null, terrain(5,2), 80));
    }

    @Test
    void t04_boardIsDeepArrayCopyWithCanonicalTilesAndValidatedCoordinates() {
        Fixture f = fixture();
        Tile[][] board = f.game.getBoard();
        Tile[][] another = f.game.getBoard();
        assertNotSame(board, another);
        for (int row = 0; row < 6; row++) {
            assertNotSame(board[row], another[row]);
            for (int col = 0; col < 5; col++) assertSame(f.tile(row,col), board[row][col]);
        }
        Tile original = f.tile(0,0);
        board[0][0] = null;
        board[1] = null;
        assertSame(original, f.tile(0,0));
        assertNotNull(f.game.getBoard()[1]);
        for (int[] coordinate : new int[][]{{-1,0},{6,0},{0,-1},{0,5}}) {
            assertFailsNoChange(IllegalArgumentException.class, f.game,
                () -> f.game.getTile(coordinate[0], coordinate[1]));
        }
    }

    @Test
    void t05_sharedBudgetCountsSuccessfulActionsAndLosesExactlyAtLimit() {
        Fixture f = fixture(terrain(5,2), 3);
        Scout scout = new Scout(f.team, f.tile(0,0));
        Medic medic = new Medic(f.team, f.tile(4,0));
        assertEquals(MissionStatus.IN_PROGRESS, f.game.getStatus());
        scout.move(f.tile(0,1));
        scout.move(f.tile(1,1)); // The same unit may act repeatedly.
        assertEquals(2, f.game.getTurnsUsed());
        assertEquals(1, f.game.getTurnsRemaining());
        assertEquals(MissionStatus.IN_PROGRESS, f.game.getStatus());
        medic.move(f.tile(4,1));
        assertEquals(3, f.game.getTurnsUsed());
        assertEquals(0, f.game.getTurnsRemaining());
        assertEquals(MissionStatus.LOST, f.game.getStatus());
        assertFailsNoChange(IllegalStateException.class, f.game, () -> scout.move(f.tile(1,2)));
    }

    @Test
    void t06_deliveryOnLastPermittedActionWinsBeforeLossEvaluation() {
        Fixture f = fixture(terrain(2,2,5,2), 2);
        Medic medic = new Medic(f.team, f.tile(2,2));
        new CommandUnit(f.team, f.tile(5,2));
        Survivor survivor = f.survivor("last",2,2);
        medic.pickUp(survivor);
        assertEquals(MissionStatus.IN_PROGRESS, f.game.getStatus());
        assertEquals(1, medic.deliver());
        assertEquals(2, f.game.getTurnsUsed());
        assertEquals(0, f.game.getTurnsRemaining());
        assertEquals(MissionStatus.WON, f.game.getStatus());
        assertTrue(medic.isUpgraded(), "delivery upgrade finishes before terminal evaluation");
        assertRescued(survivor);
    }

    @Test
    void t07_allRescuedStillRequiresCommandAtShelterAndFinalMoveMayWin() {
        Fixture f = fixture(terrain(2,2,5,2), 3);
        Scout scout = new Scout(f.team, f.tile(2,2));
        CommandUnit command = new CommandUnit(f.team, f.tile(5,3));
        Survivor survivor = f.survivor("A",2,2);
        scout.pickUp(survivor);
        scout.deliver();
        assertEquals(1, f.team.getRescuedCount());
        assertEquals(MissionStatus.IN_PROGRESS, f.game.getStatus());
        command.move(f.tile(5,2));
        assertEquals(MissionStatus.WON, f.game.getStatus());
        assertEquals(3, f.game.getTurnsUsed());
    }

    @Test
    void t08_bothTerminalStatesRejectActionsBeforeArgumentValidationAndFreezeSetup() {
        Fixture won = fixture(terrain(2,2,5,2), 5);
        Scout rescuer = new Scout(won.team, won.tile(2,2));
        new CommandUnit(won.team, won.tile(5,2));
        Survivor survivor = won.survivor("A",2,2);
        rescuer.pickUp(survivor); rescuer.deliver();
        Fixture lost = fixture(terrain(5,2), 1);
        Scout exhausted = new Scout(lost.team, lost.tile(0,0));
        exhausted.move(lost.tile(0,1));
        for (Fixture f : new Fixture[]{won,lost}) {
            for (RescueUnit unit : f.team.getUnits()) {
                assertTrue(unit.getLegalMoves().isEmpty());
                assertFailsNoChange(IllegalStateException.class, f.game, () -> unit.move(null));
                assertFailsNoChange(IllegalStateException.class, f.game, () -> unit.pickUp(null));
                assertFailsNoChange(IllegalStateException.class, f.game, unit::deliver);
            }
            assertFailsNoChange(IllegalStateException.class, f.game, () -> new Scout(f.team,f.tile(0,4)));
            assertFailsNoChange(IllegalStateException.class, f.game, () -> new Survivor("late",f.tile(0,4)));
            assertNotNull(f.game.getBoard());
            assertNotNull(f.game.getSurvivors());
            assertTrue(f.game.getTurnsRemaining() >= 0);
        }
    }

    @Test
    void t09_setupConstructorsCostNothingAndAreLockedAfterFirstSuccessfulAction() {
        Fixture f = fixture();
        Scout scout = new Scout(f.team, f.tile(0,0));
        f.survivor("A",0,1);
        assertEquals(0,f.game.getTurnsUsed());
        assertFailsNoChange(IllegalArgumentException.class, f.game, () -> scout.move(f.tile(3,3)));
        new Medic(f.team, f.tile(4,4)); // Failed actions do not lock setup.
        scout.move(f.tile(0,1));
        assertFailsNoChange(IllegalStateException.class, f.game, () -> new Scout(f.team,f.tile(3,4)));
        assertFailsNoChange(IllegalStateException.class, f.game, () -> new Medic(f.team,f.tile(3,4)));
        assertFailsNoChange(IllegalStateException.class, f.game, () -> new RescueTruck(f.team,f.tile(3,4)));
        assertFailsNoChange(IllegalStateException.class, f.game, () -> new CommandUnit(f.team,f.tile(3,4)));
        assertFailsNoChange(IllegalStateException.class, f.game, () -> new Survivor("B",f.tile(3,4)));
    }

    @Test
    void t10_completeDefaultMissionExercisesMovementCargoUpgradesWaterAndCommandWin() {
        RescueGame game = new RescueGame(new RescueTeam("Full Mission"));
        Medic medic = (Medic) game.getTile(5,1).getUnit();
        Scout scout = (Scout) game.getTile(5,0).getUnit();
        RescueTruck truck = (RescueTruck) game.getTile(5,3).getUnit();
        CommandUnit command = (CommandUnit) game.getTile(5,4).getUnit();
        Survivor a = game.getTile(0,0).getSurvivor();
        Survivor b = game.getTile(0,4).getSurvivor();
        Survivor c = game.getTile(2,4).getSurvivor();
        Survivor d = game.getTile(3,0).getSurvivor();
        moveAlong(medic,4,1,4,0,3,0); medic.pickUp(d);
        assertCarried(d,medic);
        moveAlong(medic,4,0,4,1,4,2,5,2);
        assertEquals(1,medic.deliver());
        assertTrue(medic.isUpgraded()); assertEquals(2,medic.getCapacity());
        medic.move(game.getTile(5,1));
        moveAlong(scout,4,0,3,1,2,0,1,0,0,0); scout.pickUp(a);
        moveAlong(scout,1,0,2,0,3,1,4,2,5,2);
        assertEquals(1,scout.deliver()); assertTrue(scout.isUpgraded());
        moveAlong(scout,4,1,3,1,2,1); // Water is available only after Scout's own delivery.
        assertEquals(Terrain.WATER,scout.getTile().getTerrain());
        moveAlong(truck,4,3,3,3,2,3,2,4); truck.pickUp(c);
        moveAlong(truck,2,3,1,3,0,3,0,4); truck.pickUp(b);
        assertEquals(List.of(c,b),truck.getCargo());
        moveAlong(truck,0,3,1,3,2,3,3,3,4,3,4,2,5,2);
        assertEquals(2,truck.deliver());
        assertEquals(4,game.getTeam().getRescuedCount());
        assertEquals(MissionStatus.IN_PROGRESS,game.getStatus());
        truck.move(game.getTile(4,2));
        moveAlong(command,5,3,5,2);
        assertEquals(MissionStatus.WON,game.getStatus());
        assertEquals(46,game.getTurnsUsed());
        assertEquals(34,game.getTurnsRemaining());
        assertEquals(List.of(d,a,c,b),game.getTeam().getRescuedSurvivors());
        for (Survivor survivor : game.getSurvivors()) assertRescued(survivor);
        for (RescueUnit unit : game.getTeam().getUnits()) {
            assertTrue(unit.getCargo().isEmpty()); assertTrue(unit.getLegalMoves().isEmpty());
        }
    }
}
