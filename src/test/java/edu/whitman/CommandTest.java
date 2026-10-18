package edu.whitman;

import static edu.whitman.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CommandTest {
    @Test
    void t50_commandHasEightNeighborMovementOnlyPlainShelterAndZeroCargoCapacity() {
        Terrain[][] map=terrain(5,2); map[1][1]=Terrain.RUBBLE;
        map[1][2]=Terrain.WATER; map[1][3]=Terrain.BLOCKED;
        Fixture f=fixture(map,80); CommandUnit command=new CommandUnit(f.team,f.tile(2,2));
        Survivor a=f.survivor("A",2,2);
        assertEquals(0,command.getCapacity());
        assertTrue(command.canEnter(Terrain.PLAIN)); assertTrue(command.canEnter(Terrain.SHELTER));
        assertFalse(command.canEnter(Terrain.RUBBLE)); assertFalse(command.canEnter(Terrain.WATER));
        assertFalse(command.canEnter(Terrain.BLOCKED));
        assertMoves(command,2,1,2,3,3,1,3,2,3,3);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> command.pickUp(a));
        assertFailsNoChange(IllegalArgumentException.class,f.game,command::deliver);
        assertWaiting(a,f.tile(2,2)); assertTrue(command.getCargo().isEmpty());
        assertEquals(0,command.getDeliveredCount());
    }

    @Test
    void t51_commandCanCrossBlockedCornersButCannotEnterOccupiedOrStayOnSameTile() {
        Terrain[][] map=terrain(5,2); map[1][2]=Terrain.BLOCKED; map[2][1]=Terrain.WATER;
        Fixture f=fixture(map,80); CommandUnit command=new CommandUnit(f.team,f.tile(1,1));
        new Medic(f.team,f.tile(0,0)); Survivor a=f.survivor("A",2,2);
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> command.move(f.tile(0,0)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> command.move(f.tile(1,1)));
        assertFailsNoChange(IllegalArgumentException.class,f.game,() -> command.move(f.tile(3,3)));
        command.move(f.tile(2,2));
        assertWaiting(a,f.tile(2,2)); assertTrue(command.getCargo().isEmpty());
        assertSame(command,f.tile(2,2).getUnit()); assertNull(f.tile(1,1).getUnit());
        assertEquals(1,f.game.getTurnsUsed());
    }

    @Test
    void t52_commandAtShelterDoesNotWinWhileAnySurvivorIsWaitingOrCarried() {
        Fixture f=fixture(terrain(2,2,5,2),80);
        CommandUnit command=new CommandUnit(f.team,f.tile(5,3));
        Medic medic=new Medic(f.team,f.tile(2,2)); Survivor a=f.survivor("A",2,2);
        command.move(f.tile(5,2));
        assertEquals(MissionStatus.IN_PROGRESS,f.game.getStatus()); assertWaiting(a,f.tile(2,2));
        medic.pickUp(a);
        assertEquals(MissionStatus.IN_PROGRESS,f.game.getStatus()); assertCarried(a,medic);
        medic.deliver(); assertEquals(MissionStatus.WON,f.game.getStatus());
        assertRescued(a); assertEquals(3,f.game.getTurnsUsed());
    }

    @Test
    void t53_emptySurvivorRegistryNeverWinsAndAnyOfMultipleCommandsCanSatisfyWin() {
        Fixture empty=fixture(terrain(5,2),2);
        CommandUnit emptyCommand=new CommandUnit(empty.team,empty.tile(5,3));
        emptyCommand.move(empty.tile(5,2));
        assertEquals(MissionStatus.IN_PROGRESS,empty.game.getStatus());
        emptyCommand.move(empty.tile(5,3)); assertEquals(MissionStatus.LOST,empty.game.getStatus());
        Fixture f=fixture(terrain(2,2,5,2),80);
        new CommandUnit(f.team,f.tile(0,0));
        new CommandUnit(f.team,f.tile(5,2));
        Medic medic=new Medic(f.team,f.tile(2,2)); Survivor a=f.survivor("A",2,2);
        assertEquals(MissionStatus.IN_PROGRESS,f.game.getStatus(),"constructors never evaluate success");
        medic.pickUp(a); medic.deliver();
        assertEquals(MissionStatus.WON,f.game.getStatus());
    }
}
