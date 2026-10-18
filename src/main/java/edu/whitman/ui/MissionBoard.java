package edu.whitman.ui;

import edu.whitman.RescueGame;
import edu.whitman.RescueUnit;
import edu.whitman.Tile;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** Square map cells and a single Tab stop; arrow keys explore without spending actions. */
final class MissionBoard extends JPanel {
    private static final int GAP = 6;
    private final TileButton[][] cells = new TileButton[RescueGame.HEIGHT][RescueGame.WIDTH];
    private int focusRow = 5;
    private int focusCol;

    MissionBoard(BiConsumer<Integer, Integer> onTile) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(470, 570));
        setMinimumSize(new Dimension(344, 414));
        for (int row = 0; row < RescueGame.HEIGHT; row++) {
            for (int col = 0; col < RescueGame.WIDTH; col++) {
                final int r = row;
                final int c = col;
                TileButton cell = new TileButton(row, col);
                cells[row][col] = cell;
                cell.addActionListener(event -> {
                    onTile.accept(r, c);
                    focusAt(r, c, true);
                });
                bind(cell, KeyEvent.VK_UP, () -> explore(r, c, -1, 0));
                bind(cell, KeyEvent.VK_DOWN, () -> explore(r, c, 1, 0));
                bind(cell, KeyEvent.VK_LEFT, () -> explore(r, c, 0, -1));
                bind(cell, KeyEvent.VK_RIGHT, () -> explore(r, c, 0, 1));
                bind(cell, KeyEvent.VK_ENTER, () -> cell.doClick(0));
                add(cell);
            }
        }
        focusAt(focusRow, focusCol, false);
    }

    void showGame(RescueGame game, RescueUnit selected) {
        List<Tile> legal = selected == null ? java.util.Collections.emptyList() : selected.getLegalMoves();
        boolean active = game.getStatus() == edu.whitman.MissionStatus.IN_PROGRESS;
        for (int row = 0; row < RescueGame.HEIGHT; row++) {
            for (int col = 0; col < RescueGame.WIDTH; col++) {
                Tile tile = game.getTile(row, col);
                cells[row][col].showTile(tile, selected, legal.contains(tile));
                cells[row][col].setEnabled(tile.getUnit() != null || active && selected != null);
            }
        }
        if (!cells[focusRow][focusCol].isEnabled() && selected != null) {
            focusAt(selected.getTile().getRow(), selected.getTile().getCol(), false);
        }
    }

    private void explore(int row, int col, int deltaRow, int deltaCol) {
        int r = row + deltaRow;
        int c = col + deltaCol;
        while (r >= 0 && r < RescueGame.HEIGHT && c >= 0 && c < RescueGame.WIDTH) {
            if (cells[r][c].isEnabled()) {
                focusAt(r, c, true);
                return;
            }
            r += deltaRow;
            c += deltaCol;
        }
    }

    void focusUnit(RescueUnit unit) {
        if (unit != null) focusAt(unit.getTile().getRow(), unit.getTile().getCol(), true);
    }

    private void focusAt(int row, int col, boolean requestFocus) {
        focusRow = row;
        focusCol = col;
        for (int r = 0; r < RescueGame.HEIGHT; r++) {
            for (int c = 0; c < RescueGame.WIDTH; c++) {
                cells[r][c].setFocusable(r == row && c == col);
            }
        }
        if (requestFocus && cells[row][col].isEnabled()) {
            cells[row][col].requestFocusInWindow();
            cells[row][col].scrollRectToVisible(new java.awt.Rectangle(0, 0,
                    cells[row][col].getWidth(), cells[row][col].getHeight()));
        }
    }

    private void bind(JComponent component, int key, Runnable action) {
        String name = "map-" + key;
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    @Override
    public void doLayout() {
        int size = Math.max(1, Math.min((getWidth() - GAP * 4) / 5, (getHeight() - GAP * 5) / 6));
        int left = (getWidth() - (size * 5 + GAP * 4)) / 2;
        int top = (getHeight() - (size * 6 + GAP * 5)) / 2;
        for (int row = 0; row < RescueGame.HEIGHT; row++) {
            for (int col = 0; col < RescueGame.WIDTH; col++) {
                cells[row][col].setBounds(left + col * (size + GAP), top + row * (size + GAP), size, size);
            }
        }
    }
}
