package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class MedicTest {
    @Test
    void t39_medicMovesExactlyOneOrthogonalTileAndRejectsDiagonalOrLongMoves() {
        Fixture f=fixture(); Medic medic=new Medic(f.team,f.tile(2,2));
        assertMoves(medic,1,2,2,1,2,3,3,2);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.move(f.tile(1,1)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.move(f.tile(0,2)));
        medic.move(f.tile(2,3)); assertSame(f.tile(2,3),medic.getTile());
        assertNull(f.tile(2,2).getUnit()); assertEquals(1,f.game.getTurnsUsed());
    }

    @Test
    void t40_medicOnlyEntersPlainAndShelterBeforeAndAfterUpgrade() {
        Terrain[][] map=terrain(2,2); map[1][2]=Terrain.RUBBLE;
        map[2][3]=Terrain.WATER; map[3][2]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); Medic medic=new Medic(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2);
        for (int phase=0;phase<2;phase++) {
            assertTrue(medic.canEnter(Terrain.PLAIN)); assertTrue(medic.canEnter(Terrain.SHELTER));
            assertFalse(medic.canEnter(Terrain.RUBBLE)); assertFalse(medic.canEnter(Terrain.WATER));
            assertFalse(medic.canEnter(Terrain.BLOCKED));
            assertMoves(medic,2,1);
            for (Tile tile:new Tile[]{f.tile(1,2),f.tile(2,3),f.tile(3,2)}) {
                assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.move(tile));
            }
            if (phase==0) { medic.pickUp(a); medic.deliver(); }
        }
        assertTrue(medic.isUpgraded()); assertEquals(2,medic.getCapacity());
    }

    @Test
    void t41_medicStartsAtCapacityOneAndPickupDoesNotUpgradeOrDeliver() {
        Fixture f=fixture(terrain(2,2),80); Medic medic=new Medic(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        assertFalse(medic.isUpgraded()); assertEquals(1,medic.getCapacity());
        medic.pickUp(a); medic.move(f.tile(2,3));
        assertFalse(medic.isUpgraded()); assertEquals(0,medic.getDeliveredCount());
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.pickUp(b));
        assertCarried(a,medic); assertWaiting(b,f.tile(2,3));
        assertEquals(0,f.team.getRescuedCount()); assertEquals(2,f.game.getTurnsUsed());
    }

    @Test
    void t42_firstPersonalDeliveryImmediatelyPermanentlyRaisesCapacityToTwo() {
        Fixture f=fixture(terrain(2,2),80); Medic medic=new Medic(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2);
        medic.pickUp(a); assertEquals(1,medic.getCapacity());
        assertEquals(1,medic.deliver());
        assertTrue(medic.isUpgraded()); assertEquals(2,medic.getCapacity());
        assertEquals(1,medic.getDeliveredCount()); assertEquals(2,f.game.getTurnsUsed());
        medic.move(f.tile(2,3)); medic.move(f.tile(2,2));
        assertTrue(medic.isUpgraded()); assertEquals(2,medic.getCapacity());
        assertEquals(1,medic.getDeliveredCount()); assertRescued(a);
    }

    @Test
    void t43_upgradedMedicCarriesTwoButRejectsThirdAndRescuesWholeBatch() {
        Fixture f=fixture(terrain(2,2),80); Medic medic=new Medic(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        Survivor c=f.survivor("C",3,3); Survivor d=f.survivor("D",3,2);
        medic.pickUp(a); medic.deliver();
        medic.move(f.tile(2,3)); medic.pickUp(b);
        medic.move(f.tile(3,3)); medic.pickUp(c); medic.move(f.tile(3,2));
        assertEquals(List.of(b,c),medic.getCargo());
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.pickUp(d));
        medic.move(f.tile(2,2)); int used=f.game.getTurnsUsed();
        assertEquals(2,medic.deliver()); assertEquals(used+1,f.game.getTurnsUsed());
        assertEquals(3,medic.getDeliveredCount()); assertEquals(2,medic.getCapacity());
        assertEquals(List.of(a,b,c),f.team.getRescuedSurvivors());
        assertRescued(b); assertRescued(c); assertWaiting(d,f.tile(3,2));
        assertTrue(medic.getCargo().isEmpty());
    }

    @Test
    void t44_teammateDeliveryDoesNotUpgradeMedicOrIncreaseItsCapacity() {
        Fixture f=fixture(terrain(2,2),80); Medic medic=new Medic(f.team,f.tile(0,0));
        Scout scout=new Scout(f.team,f.tile(2,2)); Survivor a=f.survivor("A",2,2);
        scout.pickUp(a); scout.deliver();
        assertTrue(scout.isUpgraded()); assertFalse(medic.isUpgraded());
        assertEquals(1,medic.getCapacity()); assertEquals(0,medic.getDeliveredCount());
        assertEquals(1,f.team.getRescuedCount());
    }

    @Test
    void t45_medicLegalMovesAtEdgesFilterOccupiedTilesButAllowWaitingSurvivors() {
        Fixture f=fixture(); Medic medic=new Medic(f.team,f.tile(0,0));
        new CommandUnit(f.team,f.tile(0,1)); Survivor a=f.survivor("A",1,0);
        assertMoves(medic,1,0);
        List<Tile> snapshot=medic.getLegalMoves(); tryToClear(snapshot);
        assertMoves(medic,1,0);
        medic.move(f.tile(1,0)); assertWaiting(a,f.tile(1,0));
        assertTrue(medic.getCargo().isEmpty());
        assertMoves(medic,0,0,1,1,2,0);
        assertEquals(1,f.game.getTurnsUsed());
    }

    @Test
    void t46_invalidMedicInteractionsNeverUpgradeAndShelterArrivalAloneDoesNotCount() {
        Fixture f=fixture(terrain(2,2),80); Medic medic=new Medic(f.team,f.tile(2,3));
        Survivor a=f.survivor("A",2,3);
        assertFailsNoChange(IllegalArgumentException.class,f.game,medic::deliver);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> medic.pickUp(null));
        medic.pickUp(a);
        assertFailsNoChange(IllegalArgumentException.class,f.game,medic::deliver);
        assertFalse(medic.isUpgraded()); assertEquals(0,medic.getDeliveredCount());
        medic.move(f.tile(2,2));
        assertFalse(medic.isUpgraded()); assertEquals(1,medic.getCapacity());
        assertEquals(0,f.team.getRescuedCount()); assertEquals(2,f.game.getTurnsUsed());
        medic.deliver();
        assertFailsNoChange(IllegalArgumentException.class,f.game,medic::deliver);
        assertTrue(medic.isUpgraded()); assertEquals(1,medic.getDeliveredCount());
        assertEquals(3,f.game.getTurnsUsed());
    }
}
