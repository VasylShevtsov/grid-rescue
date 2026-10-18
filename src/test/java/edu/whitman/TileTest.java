package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TileTest {
    @Test
    void t19_allTilesHaveCanonicalCoordinatesTerrainAndGameOwnership() {
        Terrain[][] map = terrain(5,2); map[0][0]=Terrain.RUBBLE; map[1][1]=Terrain.WATER;
        map[2][2]=Terrain.BLOCKED;
        Fixture f = fixture(map,80);
        for (int row=0;row<6;row++) for (int col=0;col<5;col++) {
            Tile tile=f.tile(row,col);
            assertSame(f.game,tile.getGame());
            assertEquals(row,tile.getRow()); assertEquals(col,tile.getCol());
            assertEquals(map[row][col],tile.getTerrain());
            assertSame(tile,f.tile(row,col));
        }
    }

    @Test
    void t20_shelterFlagExactlyMatchesTerrainAndSetupDoesNotEvaluateWinning() {
        Terrain[][] map=terrain(0,0,5,2);
        map[0][1]=Terrain.RUBBLE; map[0][2]=Terrain.WATER; map[0][3]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); new CommandUnit(f.team,f.tile(0,0));
        for (Tile[] row:f.game.getBoard()) for (Tile tile:row) {
            assertEquals(tile.getTerrain()==Terrain.SHELTER,tile.isShelter());
        }
        assertEquals(MissionStatus.IN_PROGRESS,f.game.getStatus());
        assertEquals(0,f.game.getTurnsUsed());
    }

    @Test
    void t21_unitAndSurvivorSlotsAreIndependentAndMovementDoesNotPickUp() {
        Fixture f=fixture(); Scout scout=new Scout(f.team,f.tile(1,1));
        Survivor survivor=f.survivor("A",1,2);
        scout.move(f.tile(1,2));
        assertSame(scout,f.tile(1,2).getUnit());
        assertWaiting(survivor,f.tile(1,2));
        assertTrue(scout.getCargo().isEmpty());
        assertNull(f.tile(1,1).getUnit());
        assertEquals(1,f.game.getTurnsUsed());
        scout.move(f.tile(2,2));
        assertWaiting(survivor,f.tile(1,2));
        assertNull(f.tile(1,2).getUnit());
    }

    @Test
    void t22_survivorSetupTrimsIdsAndAllowsPlainRubbleWaterAndShelter() {
        Terrain[][] map=terrain(0,3,5,2); map[0][1]=Terrain.RUBBLE; map[0][2]=Terrain.WATER;
        Fixture f=fixture(map,80);
        for (int col=0;col<4;col++) {
            Survivor survivor=f.survivor("  person-"+col+" \t",0,col);
            assertEquals("person-"+col,survivor.getId());
            assertWaiting(survivor,f.tile(0,col));
            assertTrue(f.game.getSurvivors().contains(survivor));
        }
        assertEquals(4,f.game.getSurvivors().size());
        assertEquals(0,f.game.getTurnsUsed());
    }

    @Test
    void t23_invalidSurvivorIdentityOrTileNeverRegistersOrOccupies() {
        Fixture f=fixture();
        for (String id:new String[]{null,""," ","\t\n"}) {
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor(id,f.tile(0,0)));
        }
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("A",null));
        assertFailsNoChange(IllegalArgumentException.class,f.game,
            () -> new Survivor("A",new Tile(f.game,0,0,Terrain.PLAIN)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,
            () -> new Survivor("A",new Tile(f.game,-1,0,Terrain.PLAIN)));
        assertTrue(f.game.getSurvivors().isEmpty());
        assertNull(f.tile(0,0).getSurvivor());
    }

    @Test
    void t24_duplicateTrimmedIdsAreGameWideAndFailedCreationDoesNotReserveIds() {
        Fixture f=fixture(); Survivor original=f.survivor(" A ",0,0);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("A",f.tile(1,1)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("  A\t",f.tile(2,2)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("B",f.tile(0,0)));
        Survivor b=f.survivor("B",1,1);
        assertEquals(2,f.game.getSurvivors().size());
        assertWaiting(original,f.tile(0,0)); assertWaiting(b,f.tile(1,1));
        Fixture other=fixture(); assertDoesNotThrow(() -> other.survivor("A",0,0));
    }

    @Test
    void t25_waitingSlotAndBlockedTerrainRejectPlacementAtomically() {
        Terrain[][] map=terrain(5,2); map[1][1]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); Survivor a=f.survivor("A",0,0);
        new Scout(f.team,f.tile(0,0)); // A unit does not consume the waiting-survivor slot.
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("B",f.tile(0,0)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new Survivor("C",f.tile(1,1)));
        assertWaiting(a,f.tile(0,0)); assertNull(f.tile(1,1).getSurvivor());
        assertEquals(1,f.game.getSurvivors().size());
    }

    @Test
    void t26_pickupMoveAndDeliveryMaintainExclusiveSurvivorLocations() {
        Fixture f=fixture(terrain(2,2),80);
        Scout scout=new Scout(f.team,f.tile(2,3)); Survivor survivor=f.survivor("A",2,3);
        assertWaiting(survivor,f.tile(2,3));
        scout.pickUp(survivor);
        assertCarried(survivor,scout); assertNull(f.tile(2,3).getSurvivor());
        scout.move(f.tile(2,2));
        assertCarried(survivor,scout); assertNull(f.tile(2,3).getUnit());
        assertSame(scout,f.tile(2,2).getUnit()); assertNull(f.tile(2,2).getSurvivor());
        assertEquals(1,scout.deliver());
        assertRescued(survivor); assertTrue(scout.getCargo().isEmpty());
        for (Tile[] row:f.game.getBoard()) for (Tile tile:row) assertNull(tile.getSurvivor());
        assertTrue(f.game.getSurvivors().contains(survivor));
    }
}
