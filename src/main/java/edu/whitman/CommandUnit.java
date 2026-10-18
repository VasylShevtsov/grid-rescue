package edu.whitman;

import java.util.List;

public class CommandUnit extends RescueUnit {
    public CommandUnit(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public boolean canEnter(Terrain terrain) {
        if (terrain == null) {
            throw new IllegalArgumentException("Terrain cannot be null");
            }
        return (terrain == Terrain.PLAIN || terrain == Terrain.SHELTER);
        }
    

    @Override
    public List<Tile> getLegalMoves() {
        throw new UnsupportedOperationException("TODO implement legal destinations");
    }
}
