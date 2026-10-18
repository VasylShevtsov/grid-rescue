package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UnitTest {
    @Test
    void t27_invalidMovesAreAtomicForNullForeignCounterfeitDistanceTerrainAndOccupancy() {
        Terrain[][] map=terrain(5,2); map[1][1]=Terrain.BLOCKED; map[2][1]=Terrain.WATER;
        Fixture f=fixture(map,80); Fixture other=fixture();
        Scout scout=new Scout(f.team,f.tile(2,2)); new Medic(f.team,f.tile(2,3));
        for (Tile destination:new Tile[]{null,other.tile(2,1),f.tile(2,2),f.tile(0,0),
                                       f.tile(1,1),f.tile(2,1),f.tile(2,3)}) {
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.move(destination));
        }
        assertFailsNoChange(IllegalArgumentException.class,f.game,
            () -> scout.move(new Tile(f.game,1,2,Terrain.PLAIN)));
        assertEquals(0,f.game.getTurnsUsed());
        scout.move(f.tile(1,2));
        assertEquals(1,f.game.getTurnsUsed());
        assertSame(scout,f.tile(1,2).getUnit()); assertNull(f.tile(2,2).getUnit());
    }

    @Test
    void t28_pickupRejectsNullForeignWrongTileFullCarriedAndRescuedSurvivorsAtomically() {
        Fixture f=fixture(terrain(2,2),80); Fixture other=fixture();
        Scout scout=new Scout(f.team,f.tile(2,2)); RescueTruck truck=new RescueTruck(f.team,f.tile(2,4));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        Survivor foreign=other.survivor("foreign",2,2);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(null));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(foreign));
        assertWaiting(foreign,other.tile(2,2));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(b));
        scout.pickUp(a); assertCarried(a,scout);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(a));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> truck.pickUp(a));
        scout.move(f.tile(2,3));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(b));
        assertWaiting(b,f.tile(2,3));
        scout.move(f.tile(2,2)); scout.deliver();
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> scout.pickUp(a));
        assertFailsNoChange(IllegalStateException.class,f.game,() -> new Survivor("A",f.tile(0,0)));
        assertTrue(f.game.getSurvivors().contains(a),"rescued IDs stay in registry");
        assertEquals(4,f.game.getTurnsUsed());
    }

    @Test
    void t29_deliveryRejectsEmptyOrNonShelterAndRescuesWholeCargoInOneAction() {
        Fixture f=fixture(terrain(2,2),80);
        RescueTruck truck=new RescueTruck(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        assertFailsNoChange(IllegalArgumentException.class,f.game,truck::deliver);
        truck.pickUp(a); truck.move(f.tile(2,3)); truck.pickUp(b);
        assertFailsNoChange(IllegalArgumentException.class,f.game,truck::deliver);
        assertEquals(List.of(a,b),truck.getCargo());
        truck.move(f.tile(2,2));
        int used=f.game.getTurnsUsed();
        assertEquals(2,truck.deliver());
        assertEquals(used+1,f.game.getTurnsUsed());
        assertEquals(2,truck.getDeliveredCount());
        assertEquals(List.of(a,b),f.team.getRescuedSurvivors());
        assertRescued(a); assertRescued(b); assertTrue(truck.getCargo().isEmpty());
        assertFailsNoChange(IllegalArgumentException.class,f.game,truck::deliver);
    }

    @Test
    void t30_cargoSnapshotsAreIndependentAndDoNotChangeWhenUnitActs() {
        Fixture f=fixture(terrain(2,2),80);
        RescueTruck truck=new RescueTruck(f.team,f.tile(2,2));
        Medic medic=new Medic(f.team,f.tile(0,0));
        Survivor a=f.survivor("A",2,2); Survivor b=f.survivor("B",2,3);
        List<Survivor> empty=truck.getCargo();
        truck.pickUp(a); List<Survivor> one=truck.getCargo();
        truck.move(f.tile(2,3)); truck.pickUp(b);
        assertTrue(empty.isEmpty()); assertEquals(List.of(a),one);
        List<Survivor> full=truck.getCargo(); tryToClear(full);
        assertEquals(List.of(a,b),truck.getCargo());
        truck.move(f.tile(2,2)); truck.deliver();
        assertEquals(List.of(a),one,"old snapshots remain snapshots after delivery");
        assertEquals(2,truck.getDeliveredCount()); assertEquals(0,medic.getDeliveredCount());
        assertEquals(0,medic.getCargo().size()); assertFalse(medic.isUpgraded());
    }

    @Test
    void t31_hierarchyAndReadOnlyQueriesMeetCommonUnitContractWithoutSpendingTurns() {
        Fixture f=fixture();
        RescueUnit[] units={new Scout(f.team,f.tile(1,1)),new Medic(f.team,f.tile(1,3)),
            new RescueTruck(f.team,f.tile(3,1)),new CommandUnit(f.team,f.tile(3,3))};
        assertTrue(Modifier.isAbstract(RescueUnit.class.getModifiers()));
        assertTrue(Modifier.isAbstract(UpgradeableUnit.class.getModifiers()));
        assertSame(RescueUnit.class,UpgradeableUnit.class.getSuperclass());
        assertSame(UpgradeableUnit.class,Scout.class.getSuperclass());
        assertSame(UpgradeableUnit.class,Medic.class.getSuperclass());
        assertSame(RescueUnit.class,RescueTruck.class.getSuperclass());
        assertSame(RescueUnit.class,CommandUnit.class.getSuperclass());
        List<Object> before=state(f.game);
        for (RescueUnit unit:units) {
            assertSame(f.team,unit.getTeam()); assertNotNull(unit.getTile());
            unit.getCapacity(); unit.getCargo(); unit.getDeliveredCount();
            List<Tile> snapshot=unit.getLegalMoves(); tryToClear(snapshot);
            assertFalse(unit.getLegalMoves().isEmpty());
            assertNotSame(unit.getLegalMoves(),unit.getLegalMoves());
            for (Terrain terrain:Terrain.values()) unit.canEnter(terrain);
            assertThrows(IllegalArgumentException.class,() -> unit.canEnter(null));
        }
        assertEquals(before,state(f.game));
        assertEquals(0,f.game.getTurnsUsed());
    }
}
