package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ScoutTest {
    @Test
    void t32_scoutHasExactlyEightNeighborMovesAndMayRepeatItsOwnTurns() {
        Fixture f=fixture(); Scout scout=new Scout(f.team,f.tile(2,2));
        assertMoves(scout,1,1,1,2,1,3,2,1,2,3,3,1,3,2,3,3);
        moveAlong(scout,1,1,0,0);
        assertSame(f.tile(0,0),scout.getTile()); assertEquals(2,f.game.getTurnsUsed());
        assertMoves(scout,0,1,1,0,1,1);
    }

    @Test
    void t33_scoutLegalMovesExcludeOccupancyAndRespectEveryBoardEdge() {
        Fixture f=fixture();
        Scout topLeft=new Scout(f.team,f.tile(0,0)); new Medic(f.team,f.tile(0,1));
        Scout bottomRight=new Scout(f.team,f.tile(5,4));
        f.survivor("A",1,1);
        assertMoves(topLeft,1,0,1,1);
        assertMoves(bottomRight,4,3,4,4,5,3);
        List<Tile> old=topLeft.getLegalMoves(); tryToClear(old);
        assertMoves(topLeft,1,0,1,1);
        assertEquals(0,f.game.getTurnsUsed());
    }

    @Test
    void t34_scoutInitiallyCrossesRubbleButNeverWaterOrBlockedTerrain() {
        Terrain[][] map=terrain(5,2); map[2][1]=Terrain.RUBBLE; map[1][2]=Terrain.WATER;
        map[1][1]=Terrain.BLOCKED; map[3][3]=Terrain.SHELTER;
        Fixture f=fixture(map,80); Scout scout=new Scout(f.team,f.tile(2,2));
        assertTrue(scout.canEnter(Terrain.PLAIN)); assertTrue(scout.canEnter(Terrain.RUBBLE));
        assertTrue(scout.canEnter(Terrain.SHELTER)); assertFalse(scout.canEnter(Terrain.WATER));
        assertFalse(scout.canEnter(Terrain.BLOCKED)); assertEquals(1,scout.getCapacity());
        assertFalse(scout.isUpgraded());
        assertMoves(scout,1,3,2,1,2,3,3,1,3,2,3,3);
        scout.move(f.tile(2,1)); assertSame(f.tile(2,1),scout.getTile());
        assertFalse(scout.isUpgraded());
    }

    @Test
    void t35_scoutMayMoveDiagonallyThroughBlockedCornersBecauseOnlyDestinationMatters() {
        Terrain[][] map=terrain(5,2); map[1][2]=Terrain.BLOCKED; map[2][1]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); Scout scout=new Scout(f.team,f.tile(1,1));
        Survivor survivor=f.survivor("A",2,2);
        assertTrue(scout.getLegalMoves().contains(f.tile(2,2)));
        scout.move(f.tile(2,2));
        assertWaiting(survivor,f.tile(2,2)); assertTrue(scout.getCargo().isEmpty());
        assertEquals(1,f.game.getTurnsUsed());
    }

    @Test
    void t36_scoutOwnDeliveryPermanentlyUnlocksWaterWithoutIncreasingCapacity() {
        Terrain[][] map=terrain(2,2); map[2][3]=Terrain.WATER; map[3][2]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); Scout scout=new Scout(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        assertFalse(scout.getLegalMoves().contains(f.tile(2,3)));
        scout.pickUp(a); assertFalse(scout.isUpgraded());
        assertEquals(1,scout.deliver()); assertTrue(scout.isUpgraded());
        assertEquals(1,scout.getDeliveredCount()); assertEquals(1,scout.getCapacity());
        assertTrue(scout.canEnter(Terrain.WATER)); assertFalse(scout.canEnter(Terrain.BLOCKED));
        assertTrue(scout.getLegalMoves().contains(f.tile(2,3)));
        scout.move(f.tile(2,3)); scout.pickUp(b); scout.move(f.tile(2,2)); scout.deliver();
        assertEquals(2,scout.getDeliveredCount()); assertTrue(scout.isUpgraded());
        assertEquals(1,scout.getCapacity()); assertTrue(scout.canEnter(Terrain.WATER));
        assertEquals(6,f.game.getTurnsUsed());
    }

    @Test
    void t37_otherUnitsDeliveriesDoNotUpgradeScoutOrItsPersonalDeliveryCount() {
        Terrain[][] map=terrain(2,2); map[3][1]=Terrain.WATER;
        Fixture f=fixture(map,80); Scout scout=new Scout(f.team,f.tile(2,1));
        Medic medic=new Medic(f.team,f.tile(2,2)); Survivor a=f.survivor("A",2,2);
        medic.pickUp(a); medic.deliver();
        assertTrue(medic.isUpgraded()); assertFalse(scout.isUpgraded());
        assertEquals(0,scout.getDeliveredCount()); assertEquals(1,scout.getCapacity());
        assertFalse(scout.canEnter(Terrain.WATER));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.move(f.tile(3,1)));
    }

    @Test
    void t38_scoutCannotTakeSecondSurvivorAndArrivalAtShelterDoesNotUpgradeIt() {
        Fixture f=fixture(terrain(2,2),80); Scout scout=new Scout(f.team,f.tile(2,3));
        Survivor a=f.survivor("A",2,3); Survivor b=f.survivor("B",2,2);
        scout.pickUp(a); scout.move(f.tile(2,2));
        assertFalse(scout.isUpgraded()); assertEquals(0,scout.getDeliveredCount());
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(b));
        assertWaiting(b,f.tile(2,2)); assertCarried(a,scout);
        scout.deliver();
        assertWaiting(b,f.tile(2,2)); assertTrue(scout.isUpgraded());
        scout.pickUp(b); assertEquals(1,scout.getCargo().size());
        assertEquals(1,scout.getCapacity());
    }
}
