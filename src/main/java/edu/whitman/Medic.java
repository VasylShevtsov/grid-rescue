package edu.whitman;

import java.util.List;

public class Medic extends UpgradeableUnit {
    public Medic(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    @Override
    public int getCapacity() {
        if (this.isUpgraded()) {
            return 2;
        }
        else {
            return 1;
        }
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
        return collectLegalMoves(false);
    }
}
