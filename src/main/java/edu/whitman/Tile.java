package edu.whitman;

import java.util.List;


public class Tile {

    private final RescueGame game;
    private final int row;
    private final int col;
    private final Terrain terrain;

    private RescueUnit unit;
    private Survivor survivor;

    Tile(RescueGame game, int row, int col, Terrain terrain) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        if (terrain == null) {
            throw new IllegalArgumentException("Terrain must not be null");
        }
        if (row < 0 || row >= RescueGame.WIDTH || col < 0 || col >= RescueGame.HEIGHT) {
            throw new IllegalArgumentException("Row and column must be within the board");
        }
        this.game = game;
        this.row = row;
        this.col = col;
        this.terrain = terrain;
    }

    public RescueGame getGame() {
        return game;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public boolean isShelter() {
        return terrain == Terrain.SHELTER;
    }

    public RescueUnit getUnit() {
        return unit;
    }

    public Survivor getSurvivor() {
        return survivor;
    }

}
