package edu.whitman.ui;

import edu.whitman.RescueUnit;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.KeyStroke;

/** Compact roster card, with the same unit portrait used on the board. */
final class SquadButton extends JButton {
    private final RescueUnit unit;
    private final int shortcut;
    private boolean selected;

    SquadButton(RescueUnit unit, int shortcut) {
        this.unit = unit;
        this.shortcut = shortcut;
        setPreferredSize(new Dimension(192, 60));
        setMinimumSize(new Dimension(170, 60));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setName("select-" + unit.getClass().getSimpleName());
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "select-unit");
        getActionMap().put("select-unit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { doClick(0); }
        });
    }

    void updateSelection(boolean selected) {
        this.selected = selected;
        String position = "(" + unit.getTile().getRow() + "," + unit.getTile().getCol() + ")";
        getAccessibleContext().setAccessibleName(MissionView.unitName(unit) + " at " + position
                + ", cargo " + unit.getCargo().size() + " of " + unit.getCapacity()
                + (selected ? ", selected" : ", select unit") + ", shortcut " + shortcut);
        setToolTipText("Select " + MissionView.unitName(unit) + " (" + shortcut + "). Selection is free.");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        g.setColor(selected || getModel().isRollover() ? Theme.SOFT_GREEN : Theme.PAPER);
        g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
        g.setStroke(new BasicStroke(selected || hasFocus() ? 2 : 1));
        g.setColor(selected || hasFocus() ? Theme.GREEN : Theme.LINE);
        g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 13, 13);
        GameArt.unitIcon(unit, 34).paintIcon(this, g, 10, 13);
        g.setFont(Theme.font(14, true));
        g.setColor(Theme.INK);
        g.drawString(MissionView.unitName(unit), 52, 25);
        g.setFont(Theme.font(11, false));
        g.setColor(Theme.MUTED);
        String cargo = unit.getCapacity() == 0 ? "Mission leader" : unit.getCargo().size() + "/" + unit.getCapacity() + " aboard";
        g.drawString(cargo + "  ·  " + unit.getTile().getRow() + "," + unit.getTile().getCol(), 52, 43);
        g.setFont(Theme.font(10, true));
        g.drawString(String.valueOf(shortcut), getWidth() - 17, 18);
        g.dispose();
    }
}
