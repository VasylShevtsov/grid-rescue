package edu.whitman;

/** Shared automatic upgrade state for Scout and Medic. No public upgrade setter. */
public abstract class UpgradeableUnit extends RescueUnit {
    protected UpgradeableUnit(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    public boolean isUpgraded() {
        throw new UnsupportedOperationException("TODO implement upgrade state");
    }
}
