package edu.whitman;

import java.util.ArrayList;
import java.util.List;

public class RescueGame {
    public static final int HEIGHT = 6;
    public static final int WIDTH = 5;
    public static final int DEFAULT_TURN_LIMIT = 80;

    private final RescueTeam team;
    private final Tile[][] board;
    private final int turnLimit;
    
    private final List<Survivor> survivors = new ArrayList<>();
    private int turnsUsed;
    private MissionStatus status;

    public RescueGame(RescueTeam team) {
    if (team == null) {
        throw new IllegalArgumentException("Team must not be null");
    }

    throw new UnsupportedOperationException("TODO implement default mission");
    }
    
    public RescueGame(RescueTeam team, Terrain[][] terrain, int turnLimit) {
        if (team == null) {
            throw new IllegalArgumentException("Team must not be null");
        }
        if (team.getGame() != null) {
            throw new IllegalArgumentException("A game has already been assigned to this team");
        } 
        if (terrain == null) {
            throw new IllegalArgumentException("Terrain must not be null");
        }
        if (terrain.length != HEIGHT) {
            throw new IllegalArgumentException("Terrain height must equal board height");
        }
        
        boolean hasShelter = false;
        for (int row = 0; row < HEIGHT; row++) {
            if (terrain[row].length != WIDTH) {
                throw new IllegalArgumentException("Terrain width must equal board width");
            }
            for (int col = 0; col < WIDTH; col++) {
                if (terrain[row][col] == Terrain.SHELTER) {
                    hasShelter = true;
                }
            }
        }
        if (hasShelter == false) {
            throw new IllegalArgumentException("Supplied terrain has no shelter.");
        }

        this.team = team;
        this.turnLimit = turnLimit;
        this.board = new Tile[HEIGHT][WIDTH];
        
        for (int row = 0; row < HEIGHT; row ++) {
            for (int col = 0; col < WIDTH; col++) {
                this.board[row][col] = new Tile(this, row, col, terrain[row][col]);
            }
        }

        team.bindToGame(this);
    }

    public RescueTeam getTeam() {
        return team;
    }

    public Tile getTile(int row, int col) {
        if (row < 0 || row >= HEIGHT || col < 0 || col >= WIDTH) {
            throw new IllegalArgumentException("Row and column must be within board limits");
        }
        return board[row][col];
    }

    public Tile[][] getBoard() {
        return board;
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
