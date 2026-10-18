package edu.whitman;

import java.util.List;

public class RescueTruck extends RescueUnit {
    public RescueTruck(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    @Override
    public int getCapacity() {
        return 2;
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
