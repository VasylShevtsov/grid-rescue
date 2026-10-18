 package edu.whitman;

 public abstract class UpgradeableUnit extends RescueUnit {

    protected UpgradeableUnit(RescueTeam team, Tile tile) {
        super(team, tile);
    }

    public boolean isUpgraded() {
        return this.getDeliveredCount() >= 1;
    }
}
