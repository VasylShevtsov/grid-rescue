package edu.whitman;

import java.util.List;

/**
 * Student implementation scaffold. See docs/UML_Class_Specification.txt.
 * TODO bodies intentionally fail until the required model is implemented.
 */
public class RescueGame {
    public static final int HEIGHT = 6;
    public static final int WIDTH = 5;
    public static final int DEFAULT_TURN_LIMIT = 80;

    public RescueGame(RescueTeam team) {
        throw new UnsupportedOperationException("TODO implement RescueGame");
    }

    public RescueGame(RescueTeam team, Terrain[][] terrain, int turnLimit) {
        throw new UnsupportedOperationException("TODO implement RescueGame");
    }

    public RescueTeam getTeam() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public Tile getTile(int row, int col) {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public Tile[][] getBoard() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public List<Survivor> getSurvivors() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public int getTurnLimit() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public int getTurnsUsed() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public int getTurnsRemaining() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

    public MissionStatus getStatus() {
        throw new UnsupportedOperationException("TODO implement required behavior");
    }

}
