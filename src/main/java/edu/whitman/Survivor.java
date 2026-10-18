package edu.whitman;

import java.util.List;

public class Survivor {

    private final String id;
    private final RescueGame game;

    private Tile tile;
    private RescueUnit carrier;
    private boolean rescued;

    public Survivor(String id, Tile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("tile must not be null");
        }
        tile.getGame().requiredSetupOpen();

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        if (tile.getGame().getTile(tile.getRow(), tile.getCol()) != tile) {
            throw new IllegalArgumentException("tile must belong to the game");
        }

        if (tile.getTerrain() == Terrain.BLOCKED) {
            throw new IllegalArgumentException("Tile is blocked terrain");
        }
        if (tile.getSurvivor() != null) {
            throw new IllegalArgumentException("Tile alread has a survivor");
        }

        this.id = id.trim();
        this.tile = tile;
        this.game = tile.getGame();

        this.game.registerSurvivor(this);
        this.tile.setSurvivor(this);
    }

    public String getId() {
        return id;
    }

    public Tile getTile() {
        return tile;
    }

    public RescueUnit getCarrier() {
        return carrier;
    }

    public boolean isRescued() {
        return rescued;
    }

}
