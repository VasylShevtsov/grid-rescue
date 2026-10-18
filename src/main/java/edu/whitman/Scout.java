package edu.whitman;

import java.util.List;

/** Student implementation scaffold. See the rulebook for this unit's behavior. */
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
        boolean normallyALlowed = (terrain == Terrain.PLAIN || terrain == Terrain.SHELTER || terrain == Terrain.RUBBLE);
        return normallyALlowed || (this.isUpgraded() && terrain == Terrain.WATER);
        }

    @Override
    public List<Tile> getLegalMoves() {
        throw new UnsupportedOperationException("TODO implement legal destinations");
    }
}
