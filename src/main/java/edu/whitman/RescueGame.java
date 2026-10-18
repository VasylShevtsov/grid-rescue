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
    private MissionStatus status = MissionStatus.IN_PROGRESS;

    public RescueGame(RescueTeam team) {
        this(team, createDefaultTerrain(), DEFAULT_TURN_LIMIT);

        new Scout(team, this.getTile(5, 0));
        new Medic(team, this.getTile(5, 1));
        new RescueTruck(team, this.getTile(5, 3));
        new CommandUnit(team, this.getTile(5, 4));

        new Survivor("A", this.getTile(0,0));
        new Survivor("B", this.getTile(0,4));
        new Survivor("C", this.getTile(2,4));
        new Survivor("D", this.getTile(3,0));

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
        if (turnLimit <= 0) {
            throw new IllegalArgumentException("turnLimit must be greater than zero");
        }
        
        boolean hasShelter = false;
        for (int row = 0; row < HEIGHT; row++) {
            if (terrain[row] == null || terrain[row].length != WIDTH) {
                throw new IllegalArgumentException("Each terrain row must contain WIDTH number of entries");
            }
            for (int col = 0; col < WIDTH; col++) {
                if (terrain[row][col] == null) {
                    throw new IllegalArgumentException("Each terrain entry cannot be null");
                }
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
        Tile[][] snapshot = new Tile[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) {
            snapshot[row] = board[row].clone();
        }
        return snapshot;
    }

    public List<Survivor> getSurvivors() {
        return new ArrayList<>(survivors);
    }

    public int getTurnLimit() {
        return turnLimit;
    }

    public int getTurnsUsed() {
        return turnsUsed;
    }

    public int getTurnsRemaining() {
        return Math.max(0, turnLimit - turnsUsed);
    }

    public MissionStatus getStatus() {
        return status;
    }

    //units may only be constructed before the first successful action and while the mission is in progress
    void requiredSetupOpen() {
        if (this.turnsUsed != 0 || this.status != MissionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Setup is not open");
        }
    }

    void registerSurvivor(Survivor survivor) {
        for (Survivor s : survivors) {
            if (s.getId().equals(survivor.getId())) {
                throw new IllegalArgumentException("Duplicate survivor id: " + survivor.getId());
            }
        }
        survivors.add(survivor);
    }

    private static Terrain[][] createDefaultTerrain() {
        Terrain P = Terrain.PLAIN;
        Terrain W = Terrain.WATER;
        Terrain R = Terrain.RUBBLE;
        Terrain B = Terrain.BLOCKED;
        Terrain H = Terrain.SHELTER;

        return new Terrain[][] {
            {P, P, P, P, P},
            {P, W, W, P, R},
            {R, W, B, P, P},
            {P, P, B, P, P},
            {P, P, P, P, P},
            {P, P, H, P, P}
        };
    }

}
