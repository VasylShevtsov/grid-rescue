package edu.whitman;

import java.util.List;

/**
 * Student implementation scaffold. See docs/UML_Class_Specification.txt.
 * TODO bodies intentionally fail until the required model is implemented.
 */
public abstract class RescueUnit {
    protected RescueUnit(RescueTeam team, Tile tile) {
        throw new UnsupportedOperationException("TODO implement RescueUnit");
    }

    public RescueTeam getTeam() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public Tile getTile() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public List<Survivor> getCargo() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public int getDeliveredCount() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public abstract int getCapacity();

    public abstract boolean canEnter(Terrain terrain);

    public abstract List<Tile> getLegalMoves();

    public void move(Tile destination) {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public void pickUp(Survivor survivor) {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public int deliver() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

}
