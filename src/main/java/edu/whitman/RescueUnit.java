package edu.whitman;

import java.util.ArrayList;
import java.util.List;

public abstract class RescueUnit {
    private final RescueTeam team;
    private Tile tile;
    
    private final List<Survivor> cargo = new ArrayList<>();
    private int deliveredCount;

    protected RescueUnit(RescueTeam team, Tile tile) {
        throw new UnsupportedOperationException("TODO implement RescueUnit");
    }

    public RescueTeam getTeam() {
        return team;
    }

    public Tile getTile() {
        return tile;
    }

    public List<Survivor> getCargo() {
        return new ArrayList<>(cargo);
    }

    public int getDeliveredCount() {
        return deliveredCount;
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
