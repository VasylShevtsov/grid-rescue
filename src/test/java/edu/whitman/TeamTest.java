package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

public class TeamTest {
    @Test
    void t11_teamNameIsTrimmedAndNullOrBlankNamesAreRejected() {
        assertEquals("River Rescue",new RescueTeam("  River Rescue \t").getName());
        for (String name : new String[]{null,""," ","\t\n"}) {
            assertThrows(IllegalArgumentException.class,() -> new RescueTeam(name));
        }
    }

    @Test
    void t12_newTeamIsUnboundAndOwnsIndependentEmptyRegistries() {
        RescueTeam first = new RescueTeam("First");
        RescueTeam second = new RescueTeam("Second");
        assertNull(first.getGame());
        assertTrue(first.getUnits().isEmpty());
        assertTrue(first.getRescuedSurvivors().isEmpty());
        assertEquals(0,first.getRescuedCount());
        new RescueGame(first);
        assertNull(second.getGame());
        assertTrue(second.getUnits().isEmpty());
        assertEquals(0,second.getRescuedCount());
    }

    @Test
    void t13_eachConcreteUnitRegistersExactlyOnceWithTeamAndTileAtZeroCost() {
        Fixture f = fixture();
        RescueUnit[] units = {new Scout(f.team,f.tile(0,0)),new Medic(f.team,f.tile(0,1)),
            new RescueTruck(f.team,f.tile(0,2)),new CommandUnit(f.team,f.tile(0,3))};
        assertEquals(4,f.team.getUnits().size());
        for (int i = 0; i < units.length; i++) {
            assertSame(f.team,units[i].getTeam());
            assertSame(f.tile(0,i),units[i].getTile());
            assertSame(units[i],f.tile(0,i).getUnit());
            assertTrue(f.team.getUnits().contains(units[i]));
            assertEquals(0,units[i].getDeliveredCount());
            assertTrue(units[i].getCargo().isEmpty());
        }
        assertEquals(0,f.game.getTurnsUsed());
    }

    @Test
    void t14_unitAndSurvivorRegistrySnapshotsCannotMutateOrTrackLaterRegistrations() {
        Fixture f = fixture();
        Scout first = new Scout(f.team,f.tile(0,0));
        Survivor a = f.survivor("A",1,0);
        List<RescueUnit> oldUnits = f.team.getUnits();
        List<Survivor> oldSurvivors = f.game.getSurvivors();
        new Medic(f.team,f.tile(0,1)); f.survivor("B",1,1);
        assertEquals(List.of(first),oldUnits);
        assertEquals(List.of(a),oldSurvivors);
        tryToClear(oldUnits); tryToClear(oldSurvivors);
        assertEquals(2,f.team.getUnits().size());
        assertEquals(2,f.game.getSurvivors().size());
    }

    @Test
    void t15_rescuedRegistryAppendsWholeBatchAndReturnsIndependentSnapshots() {
        Fixture f = fixture(terrain(2,2),80);
        RescueTruck truck = new RescueTruck(f.team,f.tile(2,2));
        Survivor a = f.survivor("A",2,2); Survivor b = f.survivor("B",2,3);
        List<Survivor> before = f.team.getRescuedSurvivors();
        truck.pickUp(a); truck.move(f.tile(2,3)); truck.pickUp(b);
        truck.move(f.tile(2,2)); assertEquals(2,truck.deliver());
        assertTrue(before.isEmpty());
        assertEquals(List.of(a,b),f.team.getRescuedSurvivors());
        assertEquals(2,f.team.getRescuedCount());
        assertEquals(2,f.game.getSurvivors().size(),"rescued survivors remain registered");
        List<Survivor> snapshot = f.team.getRescuedSurvivors(); tryToClear(snapshot);
        assertEquals(List.of(a,b),f.team.getRescuedSurvivors());
    }

    @Test
    void t16_invalidUnitConstructionIsAtomicForEveryConcreteType() {
        List<BiFunction<RescueTeam,Tile,RescueUnit>> factories =
            List.of(Scout::new,Medic::new,RescueTruck::new,CommandUnit::new);
        for (BiFunction<RescueTeam,Tile,RescueUnit> create : factories) {
            Terrain[][] map = terrain(5,2); map[1][1]=Terrain.BLOCKED; map[1][2]=Terrain.WATER;
            map[1][3]=Terrain.RUBBLE;
            Fixture f = fixture(map,80); Fixture foreign = fixture();
            new CommandUnit(f.team,f.tile(0,0));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(null,f.tile(0,1)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,null));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(new RescueTeam("Unbound"),f.tile(0,1)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,foreign.tile(0,1)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,f.tile(0,0)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,f.tile(1,1)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,f.tile(1,2)));
            assertFailsNoChange(IllegalArgumentException.class,f.game,
                () -> create.apply(f.team,new Tile(f.game,0,1,Terrain.PLAIN)));
            if (create != factories.get(0)) {
                assertFailsNoChange(IllegalArgumentException.class,f.game,() -> create.apply(f.team,f.tile(1,3)));
            } else {
                assertDoesNotThrow(() -> create.apply(f.team,f.tile(1,3)));
            }
            assertNull(foreign.tile(0,1).getUnit());
        }
    }

    @Test
    void t17_alreadyBoundTeamCannotBeReboundByEitherGameConstructor() {
        Fixture f = fixture(); new Scout(f.team,f.tile(0,0)); f.survivor("A",1,0);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> new RescueGame(f.team));
        assertFailsNoChange(IllegalArgumentException.class,f.game,
            () -> new RescueGame(f.team,terrain(5,2),80));
        assertSame(f.game,f.team.getGame());
        assertEquals(1,f.team.getUnits().size()); assertEquals(1,f.game.getSurvivors().size());
    }

    @Test
    void t18_customScenariosAllowRepeatedUnitTypesAndAnyCommandAtAnyShelter() {
        Fixture f = fixture(terrain(2,2,5,2),80);
        Scout scout = new Scout(f.team,f.tile(2,2));
        new Scout(f.team,f.tile(0,0));
        new RescueTruck(f.team,f.tile(0,1)); new RescueTruck(f.team,f.tile(0,2));
        new CommandUnit(f.team,f.tile(5,4)); new CommandUnit(f.team,f.tile(5,2));
        Survivor survivor = f.survivor("A",2,2);
        assertEquals(6,f.team.getUnits().size());
        scout.pickUp(survivor); scout.deliver();
        assertEquals(MissionStatus.WON,f.game.getStatus());
        assertEquals(2,f.game.getTurnsUsed());
    }
}
