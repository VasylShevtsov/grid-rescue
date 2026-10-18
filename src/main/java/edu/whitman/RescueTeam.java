package edu.whitman;

import java.util.List;
import java.util.ArrayList;

public class RescueTeam {
    private final String name;
    private RescueGame game;

    private final List<RescueUnit> units = new ArrayList<>();
    private final List<Survivor> rescuedSurvivors = new ArrayList<>();

    public RescueTeam(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name must not be null or blank");
        }

        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public RescueGame getGame() {
        return game;
    }

    public List<RescueUnit> getUnits() {
        return new ArrayList<>(units);
    }

    public List<Survivor> getRescuedSurvivors() {
        return new ArrayList<>(rescuedSurvivors);
    }

    public int getRescuedCount() {
        return rescuedSurvivors.size();
    }

}
