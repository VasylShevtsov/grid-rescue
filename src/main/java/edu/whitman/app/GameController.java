package edu.whitman.app;

import edu.whitman.Medic;
import edu.whitman.MissionStatus;
import edu.whitman.RescueGame;
import edu.whitman.RescueTeam;
import edu.whitman.RescueUnit;
import edu.whitman.Scout;
import edu.whitman.Survivor;
import edu.whitman.Tile;
import edu.whitman.UpgradeableUnit;

/**
 * Coordinates player commands and feedback without depending on a UI toolkit.
 * The model remains the authority for legal actions and mission outcomes.
 */
public final class GameController {
    private RescueGame game;
    private RescueUnit selectedUnit;
    private String message;
    private boolean error;

    public GameController() {
        this(new RescueGame(new RescueTeam("Grid Rescue")));
    }

    /** Allows tests and other views to supply a complete custom scenario. */
    public GameController(RescueGame game) {
        if (game == null) {
            throw new IllegalArgumentException("Game must not be null");
        }
        setGame(game);
    }

    public RescueGame getGame() {
        return game;
    }

    public RescueUnit getSelectedUnit() {
        return selectedUnit;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }

    /** Selection is a query operation and remains available after the mission. */
    public void selectUnit(RescueUnit unit) {
        if (unit == null || !game.getTeam().getUnits().contains(unit)) {
            reject("Choose a unit from this mission's roster.");
            return;
        }
        selectedUnit = unit;
        succeed(name(unit) + " selected. Selection does not use an action.");
    }

    public void moveTo(int row, int col) {
        Tile destination = null;
        try {
            requireSelection();
            destination = game.getTile(row, col);
            selectedUnit.move(destination);
            succeed(name(selectedUnit) + " moved to (" + row + ", " + col + ").");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            reject(moveFeedback(destination, ex.getMessage()));
        }
    }

    public void pickUp() {
        try {
            requireSelection();
            Survivor survivor = selectedUnit.getTile().getSurvivor();
            selectedUnit.pickUp(survivor);
            succeed(name(selectedUnit) + " picked up survivor " + survivor.getId() + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            String feedback = ex.getMessage();
            if (selectedUnit != null && selectedUnit.getTile().getSurvivor() == null) {
                feedback = "There is no waiting survivor on this tile. Move onto a survivor's tile first.";
            } else if (selectedUnit != null && selectedUnit.getCapacity() == 0) {
                feedback = name(selectedUnit) + " cannot carry survivors. Choose a rescue unit.";
            } else if (selectedUnit != null
                    && selectedUnit.getCargo().size() >= selectedUnit.getCapacity()) {
                feedback = "Cargo is full. Deliver at a shelter before picking up another survivor.";
            }
            reject(feedback);
        }
    }

    public void deliver() {
        try {
            requireSelection();
            boolean upgradedBefore = isUpgraded(selectedUnit);
            int delivered = selectedUnit.deliver();
            String feedback = name(selectedUnit) + " delivered " + delivered
                    + (delivered == 1 ? " survivor." : " survivors.");
            if (!upgradedBefore && isUpgraded(selectedUnit)) {
                if (selectedUnit instanceof Scout) {
                    feedback += " Scout upgraded: it can now enter water.";
                } else if (selectedUnit instanceof Medic) {
                    feedback += " Medic upgraded: it can now carry two survivors.";
                }
            }
            if (game.getStatus() == MissionStatus.IN_PROGRESS
                    && game.getTeam().getRescuedCount() == game.getSurvivors().size()) {
                feedback += " All survivors are rescued. Bring a Command Unit to a shelter.";
            }
            succeed(feedback);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            reject(ex.getMessage());
        }
    }

    /** Restart never reuses a team, unit, survivor, or board from the old game. */
    public void restart() {
        setGame(new RescueGame(new RescueTeam("Grid Rescue")));
    }

    private void setGame(RescueGame newGame) {
        game = newGame;
        selectedUnit = game.getTeam().getUnits().isEmpty()
                ? null : game.getTeam().getUnits().get(0);
        succeed(selectedUnit == null
                ? "This mission has no units. Start a new mission to play."
                : "Rescue every survivor, then bring a Command Unit to a shelter. "
                        + name(selectedUnit) + " is selected.");
    }

    private void requireSelection() {
        if (selectedUnit == null) {
            throw new IllegalArgumentException("Select a unit before taking an action.");
        }
    }

    /** Adds context only after the model has rejected a move. */
    private String moveFeedback(Tile destination, String fallback) {
        if (selectedUnit == null || destination == null) {
            return fallback;
        }
        if (destination == selectedUnit.getTile()) {
            return "That is the selected unit's current tile. Choose a highlighted destination.";
        }
        if (destination.getUnit() != null) {
            return "That tile is occupied by " + name(destination.getUnit())
                    + ". Move it first, or choose another highlighted destination.";
        }
        if (!selectedUnit.canEnter(destination.getTerrain())) {
            return name(selectedUnit) + " cannot enter "
                    + destination.getTerrain().name().toLowerCase(java.util.Locale.ROOT)
                    + ". Choose a highlighted destination.";
        }
        return "That is not a legal neighboring destination for " + name(selectedUnit)
                + ". Choose a highlighted tile.";
    }

    private void succeed(String feedback) {
        error = false;
        String outcome = outcome();
        message = outcome.isEmpty() ? feedback : feedback + " " + outcome;
    }

    private void reject(String feedback) {
        error = true;
        String outcome = outcome();
        message = outcome.isEmpty() ? feedback + " No action used." : outcome;
    }

    private String outcome() {
        if (game.getStatus() == MissionStatus.WON) {
            return "Mission won! All survivors are safe and a Command Unit is at a shelter. "
                    + "Start a new mission to play again.";
        }
        if (game.getStatus() == MissionStatus.LOST) {
            return "Mission lost: the action budget is exhausted. Start a new mission to try again.";
        }
        return "";
    }

    private static boolean isUpgraded(RescueUnit unit) {
        return unit instanceof UpgradeableUnit && ((UpgradeableUnit) unit).isUpgraded();
    }

    private static String name(RescueUnit unit) {
        return unit.getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
