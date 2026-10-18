package edu.whitman;

import java.util.ArrayList;
import java.util.List;

public abstract class RescueUnit {
    private final RescueTeam team;
    private Tile tile;
    
    private final List<Survivor> cargo = new ArrayList<>();
    private int deliveredCount;

    protected RescueUnit(RescueTeam team, Tile tile) {
        if (team == null || team.getGame() == null) {
            throw new IllegalArgumentException("Team can't be null and must belong to a game");
        }
        team.getGame().requiredSetupOpen(); //calling the team setup guard

        if (tile == null) {
            throw new IllegalArgumentException("tile can't be null");
        }
        if (team.getGame() != tile.getGame()) {
            throw new IllegalArgumentException("Tile and team must belong to the same game");
        }
        if (team.getGame().getTile(tile.getRow(), tile.getCol()) != tile) {
            throw new IllegalArgumentException("Tile must be exact object stored in the board");
        }
        if (tile.getUnit() != null) {
            throw new IllegalArgumentException("Tile unit has already been assigned");
        }
        if (!canEnter(tile.getTerrain())) {
            throw new IllegalArgumentException("Cannot enter tile");
        }

        this.team = team;
        this.tile = tile;

        team.registerUnit(this);
        tile.setUnit(this);
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

    protected List<Tile> collectLegalMoves(boolean allowDiagonals) {
        List<Tile> moves = new ArrayList<>();
        RescueGame game = team.getGame();

        if (game.getStatus() != MissionStatus.IN_PROGRESS) {
            return moves;
        }

        int currentRow = tile.getRow();
        int currentCol = tile.getCol();

        for (int delta_row = -1; delta_row <=1; delta_row++) {
            for (int delta_col = -1; delta_col <=1; delta_col++) {
                if (delta_row == 0 && delta_col == 0) {
                    continue;
                }
                if (!allowDiagonals && delta_col != 0 && delta_row != 0) {
                    continue;
                }

                int nextRow = currentRow + delta_row;
                int nextCol = currentCol + delta_col;
                if (nextRow < 0 || nextRow >= RescueGame.HEIGHT || nextCol < 0 || nextCol >= RescueGame.WIDTH) {
                    continue;
                }

                Tile destination = game.getTile(nextRow, nextCol);
                if (destination.getUnit() != null) {
                    continue;
                }
                if (!canEnter(destination.getTerrain())) {
                    continue;
                }
                
                moves.add(game.getTile(nextRow, nextCol));
            }
        }

        return moves;
    }

}
