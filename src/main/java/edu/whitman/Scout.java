package edu.whitman;

import java.util.List;

/** Student implementation scaffold. See the rulebook for this unit's behavior. */
public class Scout extends UpgradeableUnit {
    public Scout(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    @Override
    public int getCapacity() {
        throw new UnsupportedOperationException("TODO implement capacity");
    }

    @Override
    public boolean canEnter(Terrain terrain) {
        throw new UnsupportedOperationException("TODO implement terrain rules");
    }

    @Override
    public List<Tile> getLegalMoves() {
        throw new UnsupportedOperationException("TODO implement legal destinations");
    }
}
