package edu.whitman;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.function.Executable;

/** Deterministic fixtures and assertions; this is test infrastructure, not a model solution. */
final class TestSupport {
    private TestSupport() { }

    static final class Fixture {
        final RescueTeam team;
        final RescueGame game;
        Fixture(Terrain[][] terrain, int limit) {
            team = new RescueTeam("Test Rescue");
            game = new RescueGame(team, terrain, limit);
        }
        Tile tile(int row, int col) { return game.getTile(row, col); }
        Survivor survivor(String id, int row, int col) {
            return new Survivor(id, tile(row, col));
        }
    }

    static Terrain[][] terrain(int... shelters) {
        Terrain[][] result = new Terrain[6][5];
        for (Terrain[] row : result) Arrays.fill(row, Terrain.PLAIN);
        for (int i = 0; i < shelters.length; i += 2) {
            result[shelters[i]][shelters[i + 1]] = Terrain.SHELTER;
        }
        return result;
    }

    static Fixture fixture() { return new Fixture(terrain(5, 2), 80); }
    static Fixture fixture(Terrain[][] terrain, int limit) { return new Fixture(terrain, limit); }

    static void moveAlong(RescueUnit unit, int... coordinates) {
        assertEquals(0, coordinates.length % 2, "route must contain row/column pairs");
        RescueGame game = unit.getTeam().getGame();
        for (int i = 0; i < coordinates.length; i += 2) {
            unit.move(game.getTile(coordinates[i], coordinates[i + 1]));
        }
    }

    static void assertMoves(RescueUnit unit, int... coordinates) {
        List<Tile> actual = unit.getLegalMoves();
        Set<Tile> expected = new HashSet<>();
        RescueGame game = unit.getTeam().getGame();
        for (int i = 0; i < coordinates.length; i += 2) {
            expected.add(game.getTile(coordinates[i], coordinates[i + 1]));
        }
        assertEquals(expected.size(), actual.size(), "no missing destinations or duplicates");
        assertEquals(expected, new HashSet<>(actual), "exact legal destination set");
        for (Tile tile : actual) {
            assertSame(game.getTile(tile.getRow(), tile.getCol()), tile, "canonical tile required");
        }
    }

    static <T> void tryToClear(List<T> snapshot) {
        try { snapshot.clear(); } catch (UnsupportedOperationException permitted) { /* immutable copy is valid */ }
    }

    /** Flatten all externally observable mutable state, preserving object identity in lists. */
    static List<Object> state(RescueGame game) {
        List<Object> state = new ArrayList<>();
        state.add(game.getTurnsUsed());
        state.add(game.getTurnsRemaining());
        state.add(game.getStatus());
        state.add(game.getTeam().getGame());
        state.add(new ArrayList<>(game.getTeam().getUnits()));
        state.add(new ArrayList<>(game.getTeam().getRescuedSurvivors()));
        state.add(game.getTeam().getRescuedCount());
        state.add(new ArrayList<>(game.getSurvivors()));
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                Tile tile = game.getTile(row, col);
                state.add(tile);
                state.add(tile.getUnit());
                state.add(tile.getSurvivor());
            }
        }
        for (RescueUnit unit : game.getTeam().getUnits()) {
            state.add(unit.getTeam());
            state.add(unit.getTile());
            state.add(new ArrayList<>(unit.getCargo()));
            state.add(unit.getDeliveredCount());
            state.add(unit.getCapacity());
            if (unit instanceof UpgradeableUnit) state.add(((UpgradeableUnit) unit).isUpgraded());
        }
        for (Survivor survivor : game.getSurvivors()) {
            state.add(survivor.getId());
            state.add(survivor.getTile());
            state.add(survivor.getCarrier());
            state.add(survivor.isRescued());
        }
        return state;
    }

    static void assertFailsNoChange(Class<? extends Throwable> type, RescueGame game, Executable action) {
        List<Object> before = state(game);
        assertThrows(type, action);
        assertEquals(before, state(game), "rejected operation must leave all observable state unchanged");
    }

    static void assertWaiting(Survivor survivor, Tile tile) {
        assertSame(tile, survivor.getTile());
        assertSame(survivor, tile.getSurvivor());
        assertNull(survivor.getCarrier());
        assertFalse(survivor.isRescued());
    }

    static void assertCarried(Survivor survivor, RescueUnit unit) {
        assertNull(survivor.getTile());
        assertSame(unit, survivor.getCarrier());
        assertFalse(survivor.isRescued());
        assertTrue(unit.getCargo().contains(survivor));
    }

    static void assertRescued(Survivor survivor) {
        assertNull(survivor.getTile());
        assertNull(survivor.getCarrier());
        assertTrue(survivor.isRescued());
    }
}
