package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TruckTest {
    @Test
    void t47_truckHasTwoCapacityAndOnlyOrthogonalPlainOrShelterDestinations() {
        Terrain[][] map=terrain(5,2,3,2); map[1][2]=Terrain.RUBBLE;
        map[2][1]=Terrain.WATER; map[0][0]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); RescueTruck truck=new RescueTruck(f.team,f.tile(2,2));
        assertEquals(2,truck.getCapacity());
        assertTrue(truck.canEnter(Terrain.PLAIN)); assertTrue(truck.canEnter(Terrain.SHELTER));
        assertFalse(truck.canEnter(Terrain.RUBBLE)); assertFalse(truck.canEnter(Terrain.WATER));
        assertFalse(truck.canEnter(Terrain.BLOCKED));
        assertMoves(truck,2,3,3,2);
        for (Tile tile:new Tile[]{f.tile(1,2),f.tile(2,1),f.tile(1,1),f.tile(4,2)}) {
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> truck.move(tile));
        }
        truck.move(f.tile(3,2)); assertTrue(truck.getTile().isShelter());
    }

    @Test
    void t48_truckCollectsTwoAndDeliversTogetherWithNoPartialRescueOrExtraTurn() {
        Fixture f=fixture(terrain(2,2),80); RescueTruck truck=new RescueTruck(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        truck.pickUp(a); truck.move(f.tile(2,3)); truck.pickUp(b);
        assertEquals(List.of(a,b),truck.getCargo()); assertCarried(a,truck); assertCarried(b,truck);
        assertEquals(0,f.team.getRescuedCount()); assertEquals(0,truck.getDeliveredCount());
        assertFailsNoChange(IllegalArgumentException.class,f.game,truck::deliver);
        truck.move(f.tile(2,2)); assertEquals(2,truck.deliver());
        assertEquals(5,f.game.getTurnsUsed()); assertEquals(2,truck.getDeliveredCount());
        assertRescued(a); assertRescued(b); assertTrue(truck.getCargo().isEmpty());
        assertEquals(List.of(a,b),f.team.getRescuedSurvivors());
        assertEquals(2,truck.getCapacity());
    }

    @Test
    void t49_truckRejectsThirdSurvivorAndCapacityRemainsTwoAfterDelivery() {
        Fixture f=fixture(terrain(2,2),80); RescueTruck truck=new RescueTruck(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3); Survivor c=f.survivor("C",3,3);
        truck.pickUp(a); truck.move(f.tile(2,3)); truck.pickUp(b); truck.move(f.tile(3,3));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> truck.pickUp(c));
        assertWaiting(c,f.tile(3,3)); assertEquals(List.of(a,b),truck.getCargo());
        moveAlong(truck,3,2,2,2); truck.deliver();
        assertEquals(2,truck.getCapacity()); assertEquals(2,truck.getDeliveredCount());
        moveAlong(truck,2,3,3,3); truck.pickUp(c);
        assertCarried(c,truck); assertEquals(2,truck.getCapacity());
        assertEquals(2,truck.getDeliveredCount()); assertEquals(2,f.team.getRescuedCount());
    }
}
