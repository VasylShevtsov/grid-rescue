package edu.whitman.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/** In-window rules, available without losing progress or opening another window. */
final class GameHelpPanel extends JPanel {
    private final JButton back = Theme.button("Back to mission");

    GameHelpPanel(Runnable close) {
        setName("help-panel");
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        JPanel header = MissionView.transparent(new BorderLayout(16, 0));
        JPanel introduction = MissionView.vertical();
        introduction.add(Theme.label("A rescue in four steps.", 30, true));
        introduction.add(Box.createVerticalStrut(6));
        introduction.add(Theme.muted("No timer. One shared action budget. A whole team to bring home.", 15));
        header.add(introduction, BorderLayout.CENTER);
        JPanel buttonHolder = MissionView.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        back.setName("close-help");
        back.setIcon(GameArt.icon("back", 18));
        back.addActionListener(event -> close.run());
        buttonHolder.add(back);
        header.add(buttonHolder, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new HelpContent();
        JPanel steps = MissionView.transparent(new GridLayout(2, 2, 16, 12));
        steps.add(step("1", "Choose a specialist", "Select a unit from the roster or click its piece on the map. You may use the same unit again and again.", "scout"));
        steps.add(step("2", "Move one tile", "Green outlines mark legal moves. Click a destination. Coordinates are (row,column), starting at (0,0) in the top-left.", "arrow"));
        steps.add(step("3", "Give someone a lift", "Move onto a survivor, then choose Pick up survivor. Arriving does not collect them automatically. You need a free seat.", "pickup"));
        steps.add(step("4", "Bring them home", "Reach shelter at (5,2), then Deliver cargo. Everyone aboard is rescued in one action. Move away to make room for the next unit.", "deliver"));
        content.add(steps, BorderLayout.NORTH);
        JPanel reference = MissionView.transparent(new GridLayout(1, 2, 16, 0));
        reference.add(unitGuide());
        reference.add(missionRules());
        content.add(reference, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setName("help-scroll");
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        Theme.styleScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);
        JPanel keyboard = Theme.card(new BorderLayout(16, 0));
        keyboard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        keyboard.add(Theme.label("Keyboard", 15, true), BorderLayout.WEST);
        keyboard.add(Theme.label("<html><b>1–4</b> Select &nbsp; <b>P</b> Pick up &nbsp; <b>D</b> Deliver &nbsp; <b>H</b> Help &nbsp; <b>Esc</b> Back<br>"
                + "<b>Tab / Shift+Tab</b> Change control &nbsp; <b>Arrow keys</b> Explore map &nbsp; <b>Enter / Space</b> Activate</html>", 13, false), BorderLayout.CENTER);
        add(keyboard, BorderLayout.SOUTH);
    }

    private JPanel step(String number, String title, String instruction, String icon) {
        JPanel panel = Theme.card(new BorderLayout(13, 0));
        JLabel glyph = new JLabel(GameArt.icon(icon, 27, Theme.GREEN));
        glyph.setVerticalAlignment(JLabel.TOP);
        panel.add(glyph, BorderLayout.WEST);
        JPanel copy = MissionView.vertical();
        copy.add(Theme.label(number + "  " + title, 17, true));
        copy.add(Box.createVerticalStrut(6));
        copy.add(Theme.label("<html><div style='width:270px'>" + instruction + "</div></html>", 13, false));
        panel.add(copy, BorderLayout.CENTER);
        return panel;
    }

    private JPanel unitGuide() {
        JPanel panel = Theme.card(new BorderLayout(0, 12));
        panel.add(Theme.label("Meet your team", 19, true), BorderLayout.NORTH);
        JPanel rows = MissionView.transparent(new GridLayout(4, 1, 0, 8));
        rows.add(unit("scout", "Scout", "8 directions · 1 seat · Crosses rubble.<br>First delivery unlocks water."));
        rows.add(unit("medic", "Medic", "4 directions · 1 seat · Plain and shelter.<br>First delivery gives you a second seat."));
        rows.add(unit("truck", "Rescue truck", "4 directions · 2 seats · Plain and shelter.<br>Bring two survivors home in one delivery."));
        rows.add(unit("command", "Command", "8 directions · No cargo · Plain and shelter.<br>Must reach shelter to complete the mission."));
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JPanel unit(String icon, String name, String text) {
        JPanel row = MissionView.transparent(new BorderLayout(12, 0));
        row.add(new JLabel(GameArt.icon(icon, 23, Theme.GREEN)), BorderLayout.WEST);
        row.add(Theme.label("<html><b>" + name + "</b><br>" + text + "</html>", 12, false), BorderLayout.CENTER);
        return row;
    }

    private JPanel missionRules() {
        JPanel panel = Theme.card(new BorderLayout(0, 12));
        panel.add(Theme.label("What counts", 19, true), BorderLayout.NORTH);
        JLabel rules = Theme.label("<html><div style='width:288px'>"
                + "<b>Every action matters.</b> Each successful move, pickup and delivery costs 1 of your 80 actions. Selection, help and rejected moves are free."
                + "<br><br><b>Terrain has rules.</b> Plain is labeled Field on the map. Blocked tiles are always off limits. Only Scout crosses rubble; upgraded Scout also crosses water. Occupied tiles select a unit instead of moving."
                + "<br><br><b>Make room.</b> Only one unit can occupy a tile. Diagonal moves check only the destination. Clear the shelter after each delivery."
                + "<br><br><b>Finish together.</b> Rescue every survivor and bring Command to shelter. Winning on the last action counts. Run out first and the mission is lost."
                + "<br><br><b>Start fresh.</b> New mission replaces the whole game. We ask before discarding an unfinished mission.</div></html>", 13, false);
        rules.setVerticalAlignment(JLabel.TOP);
        panel.add(rules, BorderLayout.CENTER);
        return panel;
    }

    void focusBackButton() { back.requestFocusInWindow(); }

    /** Preserve the full rules at every window height, with scrolling when needed. */
    private static final class HelpContent extends JPanel implements Scrollable {
        HelpContent() {
            super(new BorderLayout(0, 18));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 8));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
