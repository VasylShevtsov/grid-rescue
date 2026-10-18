package edu.whitman;

import java.util.List;

public class Scout extends UpgradeableUnit {
    public Scout(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    @Override
    public int getCapacity() {
        return 1;
    }

    @Override
    public boolean canEnter(Terrain terrain) {
        if (terrain == null) {
            throw new IllegalArgumentException("Terrain cannot be null");
            }
        boolean normallyAllowed = (terrain == Terrain.PLAIN || terrain == Terrain.SHELTER || terrain == Terrain.RUBBLE);
        return normallyAllowed || (this.isUpgraded() && terrain == Terrain.WATER);
        }

    @Override
    public List<Tile> getLegalMoves() {
        return collectLegalMoves(true);
    }
}
