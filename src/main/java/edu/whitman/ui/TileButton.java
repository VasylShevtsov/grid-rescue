package edu.whitman.ui;

import edu.whitman.RescueUnit;
import edu.whitman.Terrain;
import edu.whitman.Tile;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JButton;

/** A compact illustrated map cell with separate terrain, occupancy and action cues. */
final class TileButton extends JButton {
    private Tile tile;
    private boolean legal;
    private boolean selected;
    private static final Color CORAL = new Color(0xA84632);

    TileButton(int row, int col) {
        setName("tile-" + row + "-" + col);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setPreferredSize(new Dimension(82, 82));
        setMinimumSize(new Dimension(64, 64));
    }

    void showTile(Tile tile, RescueUnit selectedUnit, boolean legal) {
        this.tile = tile;
        this.legal = legal;
        this.selected = tile.getUnit() != null && tile.getUnit() == selectedUnit;
        String description = "Row " + tile.getRow() + ", column " + tile.getCol()
                + " (" + tile.getRow() + "," + tile.getCol() + "), "
                + terrainLabel(tile.getTerrain()) + " (" + tile.getTerrain().name() + ")"
                + (tile.getUnit() == null ? "" : ", " + MissionView.unitName(tile.getUnit()))
                + (tile.getSurvivor() == null ? "" : ", survivor " + tile.getSurvivor().getId());
        String action;
        if (selected) {
            action = "Selected unit. Choose a highlighted tile to move.";
        } else if (tile.getUnit() != null) {
            action = "Select " + MissionView.unitName(tile.getUnit()) + ".";
        } else if (legal) {
            action = "Move " + MissionView.unitName(selectedUnit) + " here. Uses 1 action.";
        } else {
            action = "Not a legal destination for the selected unit.";
        }
        String state = selected ? ", selected unit" : legal ? ", legal destination" : "";
        getAccessibleContext().setAccessibleName(description + state + ". " + action);
        getAccessibleContext().setAccessibleDescription(state + ". " + action);
        setToolTipText(description + ". " + action);
        setCursor(Cursor.getPredefinedCursor(legal || tile.getUnit() != null
                ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (tile == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        boolean compact = Math.min(width, height) < 75;
        boolean hover = isEnabled() && getModel().isRollover();
        Shape cell = new RoundRectangle2D.Float(2, 2, width - 4, height - 4, 14, 14);

        g.setColor(background(tile.getTerrain()));
        g.fill(cell);
        Shape oldClip = g.getClip();
        g.clip(cell);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                tile.getTerrain() == Terrain.WATER ? .24f : .10f));
        String texture = tile.getTerrain() == Terrain.WATER ? "water" : "grass";
        GameArt.sprite(g, texture, 0, 0, Math.max(width, height));
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(oldClip);
        if (hover) {
            g.setColor(new Color(255, 255, 255, 65));
            g.fill(cell);
        }

        g.setColor(selected ? Theme.GREEN : legal ? new Color(0x77A58A) : new Color(0xD0D9CA));
        g.setStroke(new BasicStroke(selected ? 3f : legal ? 2f : 1f));
        g.draw(cell);
        if (selected) {
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(5, 5, width - 10, height - 10, 10, 10);
        }

        g.setFont(Theme.font(compact ? 8 : 9, false));
        g.setColor(new Color(0x526B59));
        g.drawString(tile.getRow() + "," + tile.getCol(), 8, compact ? 13 : 15);

        if (tile.getUnit() != null) {
            int size = compact ? 22 : Math.min(34, height - 46);
            Icon unit = GameArt.unitIcon(tile.getUnit(), size);
            int y = height - 34 - size;
            unit.paintIcon(this, g, (width - size) / 2, y);
            g.setFont(Theme.font(compact ? 10 : 11, true));
            g.setColor(Theme.INK);
            centered(g, GameArt.unitLabel(tile.getUnit()), height - 20);
        } else if (tile.getSurvivor() != null) {
            int size = compact ? 22 : 32;
            int x = (width - size) / 2;
            int y = height - 34 - size;
            g.setColor(new Color(0xFAE2D6));
            g.fillOval(x, y, size, size);
            GameArt.icon("person", size - 8, CORAL).paintIcon(this, g, x + 4, y + 4);
            g.setFont(Theme.font(compact ? 9 : 10, true));
            g.setColor(CORAL);
            centered(g, "Help needed", height - 20);
        } else {
            paintTerrain(g, compact, width, height);
        }

        if (tile.getSurvivor() != null) {
            String id = tile.getSurvivor().getId();
            g.setFont(Theme.font(compact ? 10 : 11, true));
            int badgeWidth = Math.max(19, g.getFontMetrics().stringWidth(id) + 10);
            int x = width - badgeWidth - 5;
            g.setColor(CORAL);
            g.fillRoundRect(x, 5, badgeWidth, 19, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString(id, x + (badgeWidth - g.getFontMetrics().stringWidth(id)) / 2, 18);
        }

        g.setFont(Theme.font(compact ? 9 : 10, tile.isShelter()));
        g.setColor(tile.isShelter() ? new Color(0x285B44) : new Color(0x4E6257));
        centered(g, terrainLabel(tile.getTerrain()), height - 7);

        if (selected && tile.getSurvivor() == null) {
            GameArt.icon("check", 12, Theme.GREEN).paintIcon(this, g, width - 20, 6);
        } else if (legal) {
            g.setColor(Theme.GREEN);
            // The survivor badge owns the upper corner; keep its move marker below it.
            g.fillOval(width - 16, tile.getSurvivor() == null ? 7 : 29, 8, 8);
        }
        if (hasFocus()) {
            g.setColor(Theme.INK);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10, new float[] {2, 3}, 0));
            g.drawRoundRect(6, 6, width - 12, height - 12, 8, 8);
        }
        g.dispose();
    }

    private void paintTerrain(Graphics2D g, boolean compact, int width, int height) {
        int size = compact ? 28 : Math.min(40, height - 32);
        String sprite;
        switch (tile.getTerrain()) {
            case RUBBLE: sprite = "rubble"; break;
            case BLOCKED: sprite = "blocked"; break;
            case SHELTER: sprite = "shelter"; break;
            case WATER:
                GameArt.icon("water", compact ? 22 : 28, new Color(0x398393))
                        .paintIcon(this, g, (width - (compact ? 22 : 28)) / 2, height / 2 - 14);
                return;
            default: return; // Leave traversable fields open, with the subtle ground texture.
        }
        GameArt.sprite(g, sprite, (width - size) / 2, (height - size) / 2 - 5, size);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void centered(Graphics2D g, String text, int y) {
        g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    static String terrainLabel(Terrain terrain) {
        switch (terrain) {
            case SHELTER: return "Shelter";
            case WATER: return "Water";
            case RUBBLE: return "Rubble";
            case BLOCKED: return "Blocked";
            default: return "Field";
        }
    }

    private Color background(Terrain terrain) {
        switch (terrain) {
            case SHELTER: return new Color(0xD7E8CB);
            case WATER: return new Color(0xD5EBED);
            case RUBBLE: return new Color(0xEEDCC1);
            case BLOCKED: return new Color(0xD4D9D3);
            default: return new Color(0xECF0DC);
        }
    }
}
