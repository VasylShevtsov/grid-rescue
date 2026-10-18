package edu.whitman.ui;

import edu.whitman.CommandUnit;
import edu.whitman.MissionStatus;
import edu.whitman.RescueGame;
import edu.whitman.RescueTruck;
import edu.whitman.RescueUnit;
import edu.whitman.Scout;
import edu.whitman.Survivor;
import edu.whitman.Tile;
import edu.whitman.UpgradeableUnit;
import edu.whitman.app.GameController;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;

/** A playable view; the controller and model remain the authority for game state. */
public final class MissionView extends JPanel implements Scrollable {
    private final GameController controller;
    private final MissionBoard board;
    private final JPanel surfaces = Theme.panel(new CardLayout(), Theme.BACKGROUND);
    private final JPanel missionHeader = Theme.panel(new CardLayout(), Theme.BACKGROUND);
    private final JPanel roster = Theme.panel(new GridLayout(0, 1, 0, 8), Theme.BACKGROUND);
    private final List<SquadButton> unitButtons = new ArrayList<>();
    private final JLabel remaining = Theme.label("", 23, true);
    private final JLabel actionsCaption = Theme.muted("", 12);
    private final JLabel rescued = Theme.label("", 23, true);
    private final JLabel rescuedCaption = Theme.muted("survivors home", 12);
    private final JProgressBar budget = new JProgressBar();
    private final JLabel detail = Theme.label("", 12, false);
    private final JLabel portrait = Theme.label("", 20, true);
    private final JLabel survivors = Theme.label("", 13, false);
    private final JLabel destinations = Theme.muted("", 11);
    private final JLabel contextHint = Theme.label("", 13, false);
    private final JLabel resultTitle = Theme.label("", 25, true);
    private final JLabel resultSummary = Theme.label("", 13, false);
    private final JLabel restartSummary = Theme.label("", 15, false);
    private final JTextArea feedback = new JTextArea(2, 60);
    private final JPanel feedbackPanel = Theme.card(new BorderLayout(12, 0));
    private final JLabel feedbackIcon = new JLabel();
    private final JButton pickup = Theme.button("Pick up survivor");
    private final JButton deliver = Theme.button("Deliver cargo");
    private final JButton restart = Theme.button("New mission");
    private final JButton help = Theme.button("How to play");
    private final JButton playAgain = Theme.button("Play again");
    private final JButton cancelRestart = Theme.button("Keep playing");
    private final JPanel resultPanel;
    private final JPanel helpPanel;
    private final JPanel restartPanel;
    private String surface = "game";
    private Component previousFocus;
    private RescueGame renderedGame;

    public MissionView(GameController controller) {
        if (controller == null) throw new IllegalArgumentException("A controller is required");
        this.controller = controller;
        board = new MissionBoard(this::activateTile);
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(1160, 820));
        help.setName("help");
        restart.setName("restart");
        pickup.setName("pickup");
        deliver.setName("deliver");
        detail.setName("selected-details");
        survivors.setName("survivors");
        contextHint.setName("context-hint");
        destinations.setName("destinations");
        remaining.setName("actions-remaining");
        rescued.setName("rescued-count");
        add(navigation(), BorderLayout.NORTH);
        surfaces.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        resultPanel = result();
        helpPanel = new GameHelpPanel(this::closeSurface);
        restartPanel = restartConfirmation();
        surfaces.add(gameSurface(), "game");
        surfaces.add(helpPanel, "help");
        surfaces.add(restartPanel, "restart");
        ((CardLayout) surfaces.getLayout()).show(surfaces, "game");
        add(surfaces, BorderLayout.CENTER);
        pickup.addActionListener(event -> act(controller::pickUp));
        deliver.addActionListener(event -> act(controller::deliver));
        restart.addActionListener(event -> requestRestart());
        help.addActionListener(event -> openSurface("help"));
        playAgain.addActionListener(event -> freshMission());
        bind(KeyEvent.VK_P, () -> { if (playingSurface()) pickup.doClick(0); });
        bind(KeyEvent.VK_D, () -> { if (playingSurface()) deliver.doClick(0); });
        bind(KeyEvent.VK_H, () -> { if ("help".equals(surface)) closeSurface(); else if (playingSurface()) openSurface("help"); });
        bind(KeyEvent.VK_ESCAPE, this::closeSurface);
        for (int index = 0; index < 4; index++) {
            final int selectedIndex = index;
            bind(KeyEvent.VK_1 + index, () -> {
                if (playingSurface() && selectedIndex < unitButtons.size()) {
                    unitButtons.get(selectedIndex).doClick(0);
                    board.focusUnit(controller.getSelectedUnit());
                }
            });
        }
        refresh();
    }

    private JPanel navigation() {
        JPanel bar = Theme.panel(new BorderLayout(20, 0), Theme.PAPER);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(11, 24, 11, 24)));
        JLabel brand = Theme.label("Grid Rescue", 24, true);
        brand.setIcon(GameArt.icon("shield", 30, Theme.GREEN));
        brand.setIconTextGap(11);
        bar.add(brand, BorderLayout.WEST);
        JLabel tagline = Theme.muted("A small team. A big rescue.", 13);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        bar.add(tagline, BorderLayout.CENTER);
        JPanel controls = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        help.setIcon(GameArt.icon("help", 17));
        help.setToolTipText("Rules and keyboard shortcuts (H). Your mission stays paused while reading.");
        restart.setIcon(GameArt.icon("restart", 17));
        restart.setToolTipText("Start a fresh mission. We will ask before discarding an unfinished mission.");
        controls.add(help);
        controls.add(restart);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private JPanel gameSurface() {
        JPanel game = transparent(new BorderLayout(0, 10));
        missionHeader.add(missionStatus(), "status");
        missionHeader.add(resultPanel, "result");
        game.add(missionHeader, BorderLayout.NORTH);
        JPanel workspace = transparent(new BorderLayout(16, 0));
        workspace.add(teamColumn(), BorderLayout.WEST);
        workspace.add(mapCard(), BorderLayout.CENTER);
        workspace.add(inspector(), BorderLayout.EAST);
        game.add(workspace, BorderLayout.CENTER);
        game.add(feedback(), BorderLayout.SOUTH);
        return game;
    }

    private JPanel missionStatus() {
        JPanel status = transparent(new BorderLayout(20, 0));
        JPanel objective = vertical();
        objective.add(Theme.label("Bring everyone home.", 25, true));
        objective.add(Box.createVerticalStrut(5));
        objective.add(Theme.muted("Rescue every survivor, then bring Command to shelter.", 13));
        status.add(objective, BorderLayout.CENTER);
        JPanel meters = transparent(new GridLayout(1, 2, 24, 0));
        JPanel rescueMeter = vertical();
        rescueMeter.add(rescued);
        rescueMeter.add(rescuedCaption);
        meters.add(rescueMeter);
        JPanel actionMeter = vertical();
        actionMeter.add(remaining);
        actionMeter.add(actionsCaption);
        budget.setPreferredSize(new Dimension(158, 5));
        budget.setMaximumSize(new Dimension(158, 5));
        budget.setForeground(Theme.GREEN);
        budget.setBackground(Theme.LINE);
        budget.setBorderPainted(false);
        actionMeter.add(Box.createVerticalStrut(6));
        actionMeter.add(budget);
        meters.add(actionMeter);
        status.add(meters, BorderLayout.EAST);
        status.setPreferredSize(new Dimension(1000, 62));
        return status;
    }

    private JPanel teamColumn() {
        JPanel team = transparent(new BorderLayout(0, 14));
        team.setPreferredSize(new Dimension(204, 480));
        JPanel squad = vertical();
        squad.add(Theme.label("YOUR TEAM", 11, true));
        squad.add(Box.createVerticalStrut(10));
        roster.setAlignmentX(LEFT_ALIGNMENT);
        squad.add(roster);
        team.add(squad, BorderLayout.NORTH);
        JPanel registry = Theme.card(new BorderLayout(0, 10));
        registry.setBorder(BorderFactory.createEmptyBorder(15, 13, 15, 13));
        registry.add(Theme.label("People to bring home", 14, true), BorderLayout.NORTH);
        registry.add(survivors, BorderLayout.CENTER);
        survivors.setVerticalAlignment(SwingConstants.TOP);
        JPanel lower = transparent(new BorderLayout());
        lower.add(registry, BorderLayout.NORTH);
        team.add(lower, BorderLayout.CENTER);
        return team;
    }

    private JPanel mapCard() {
        JPanel card = Theme.card(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(13, 14, 12, 14));
        JPanel title = transparent(new BorderLayout());
        title.add(Theme.label("The rescue zone", 16, true), BorderLayout.WEST);
        title.add(Theme.muted("6 × 5", 12), BorderLayout.EAST);
        card.add(title, BorderLayout.NORTH);
        card.add(board, BorderLayout.CENTER);
        JPanel caption = vertical();
        caption.add(Theme.muted("Green outline + dot = a legal move", 11));
        destinations.setAlignmentX(LEFT_ALIGNMENT);
        caption.add(destinations);
        card.add(caption, BorderLayout.SOUTH);
        return card;
    }

    private JPanel inspector() {
        JPanel column = transparent(new BorderLayout(0, 12));
        column.setPreferredSize(new Dimension(252, 480));
        JPanel selected = Theme.card(new BorderLayout(0, 10));
        selected.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel heading = vertical();
        heading.add(Theme.muted("SELECTED UNIT", 10));
        heading.add(Box.createVerticalStrut(7));
        portrait.setIconTextGap(10);
        heading.add(portrait);
        selected.add(heading, BorderLayout.NORTH);
        selected.add(detail, BorderLayout.CENTER);
        detail.setVerticalAlignment(SwingConstants.TOP);
        column.add(selected, BorderLayout.NORTH);
        JPanel actions = Theme.card(new BorderLayout(0, 10));
        actions.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JPanel guide = vertical();
        guide.add(Theme.muted("YOUR NEXT MOVE", 10));
        guide.add(Box.createVerticalStrut(8));
        guide.add(contextHint);
        actions.add(guide, BorderLayout.NORTH);
        JPanel controls = transparent(new GridLayout(0, 1, 0, 8));
        pickup.setHorizontalAlignment(SwingConstants.LEFT);
        deliver.setHorizontalAlignment(SwingConstants.LEFT);
        controls.add(pickup);
        controls.add(deliver);
        actions.add(controls, BorderLayout.CENTER);
        actions.add(Theme.muted("<html>Each successful action costs 1.<br>Rejected moves cost nothing.</html>", 12), BorderLayout.SOUTH);
        JPanel lower = transparent(new BorderLayout(0, 12));
        lower.add(actions, BorderLayout.NORTH);
        column.add(lower, BorderLayout.CENTER);
        return column;
    }

    private JPanel feedback() {
        feedbackPanel.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        feedbackIcon.setVerticalAlignment(SwingConstants.TOP);
        feedbackPanel.add(feedbackIcon, BorderLayout.WEST);
        JPanel copy = vertical();
        copy.add(Theme.muted("MISSION LOG", 10));
        copy.add(Box.createVerticalStrut(4));
        feedback.setName("feedback");
        feedback.setEditable(false);
        feedback.setFocusable(false);
        feedback.setOpaque(false);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
        feedback.setFont(Theme.font(13, false));
        feedback.setAlignmentX(LEFT_ALIGNMENT);
        feedback.setBorder(BorderFactory.createEmptyBorder());
        feedback.getAccessibleContext().setAccessibleName("Latest action and mission feedback");
        copy.add(feedback);
        feedbackPanel.add(copy, BorderLayout.CENTER);
        return feedbackPanel;
    }

    private JPanel result() {
        JPanel result = Theme.card(new BorderLayout(16, 0));
        result.setName("result-panel");
        result.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 14));
        JPanel copy = vertical();
        resultTitle.setName("result-title");
        copy.add(resultTitle);
        copy.add(resultSummary);
        result.add(copy, BorderLayout.CENTER);
        playAgain.setName("play-again");
        playAgain.setIcon(GameArt.icon("restart", 17, Color.WHITE));
        Theme.primary(playAgain, true);
        JPanel control = transparent(new GridBagLayout());
        control.add(playAgain);
        result.add(control, BorderLayout.EAST);
        result.setPreferredSize(new Dimension(1000, 66));
        return result;
    }

    private JPanel restartConfirmation() {
        JPanel page = transparent(new GridBagLayout());
        page.setName("restart-panel");
        JPanel card = Theme.card(new BorderLayout(0, 22));
        card.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));
        JPanel copy = vertical();
        JLabel icon = new JLabel(GameArt.icon("restart", 40, Theme.GREEN));
        copy.add(icon);
        copy.add(Box.createVerticalStrut(18));
        copy.add(Theme.label("Start a fresh mission?", 28, true));
        copy.add(Box.createVerticalStrut(12));
        copy.add(restartSummary);
        copy.add(Box.createVerticalStrut(12));
        copy.add(Theme.muted("Your current mission will be replaced. This cannot be undone.", 14));
        card.add(copy, BorderLayout.CENTER);
        JPanel buttons = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        cancelRestart.setName("cancel-restart");
        cancelRestart.addActionListener(event -> closeSurface());
        JButton confirm = Theme.button("Start new mission");
        confirm.setName("confirm-restart");
        Theme.primary(confirm, true);
        confirm.addActionListener(event -> freshMission());
        buttons.add(cancelRestart);
        buttons.add(confirm);
        card.add(buttons, BorderLayout.SOUTH);
        page.add(card);
        return page;
    }

    private void activateTile(int row, int col) {
        if (!playingSurface()) return;
        Tile tile = controller.getGame().getTile(row, col);
        if (tile.getUnit() != null) act(() -> controller.selectUnit(tile.getUnit()));
        else act(() -> controller.moveTo(row, col));
    }

    private void act(Runnable action) {
        if (!playingSurface()) return;
        action.run();
        refresh();
    }

    private boolean playingSurface() { return "game".equals(surface); }

    private void requestRestart() {
        if (!playingSurface()) return;
        RescueGame game = controller.getGame();
        if (game.getStatus() == MissionStatus.IN_PROGRESS && game.getTurnsUsed() > 0) {
            restartSummary.setText(actionCount(game.getTurnsUsed()) + " played · " + game.getTeam().getRescuedCount()
                    + " of " + game.getSurvivors().size() + " survivors rescued");
            openSurface("restart");
        } else freshMission();
    }

    private void freshMission() {
        controller.restart();
        surface = "game";
        ((CardLayout) surfaces.getLayout()).show(surfaces, surface);
        help.setEnabled(true);
        restart.setEnabled(true);
        refresh();
        if (!unitButtons.isEmpty()) unitButtons.get(0).requestFocusInWindow();
    }

    private void openSurface(String next) {
        if (!playingSurface()) return;
        previousFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        surface = next;
        ((CardLayout) surfaces.getLayout()).show(surfaces, next);
        help.setEnabled(false);
        restart.setEnabled(false);
        if ("restart".equals(next)) cancelRestart.requestFocusInWindow();
        else ((GameHelpPanel) helpPanel).focusBackButton();
    }

    private void closeSurface() {
        if (playingSurface()) return;
        surface = "game";
        ((CardLayout) surfaces.getLayout()).show(surfaces, surface);
        help.setEnabled(true);
        restart.setEnabled(true);
        if (previousFocus != null && previousFocus.isShowing() && previousFocus.isEnabled()) previousFocus.requestFocusInWindow();
        else help.requestFocusInWindow();
    }

    private void refresh() {
        RescueGame game = controller.getGame();
        RescueUnit selected = controller.getSelectedUnit();
        boolean active = game.getStatus() == MissionStatus.IN_PROGRESS;
        remaining.setText(game.getTurnsRemaining() + " left");
        actionsCaption.setText(game.getTurnsUsed() + " used · " + game.getTurnLimit() + " total actions");
        remaining.setToolTipText(game.getTurnsUsed() + " used of " + game.getTurnLimit() + " actions");
        remaining.getAccessibleContext().setAccessibleName(game.getTurnsRemaining() + " actions remaining, "
                + game.getTurnsUsed() + " used of " + game.getTurnLimit());
        remaining.setForeground(game.getTurnsRemaining() <= Math.max(1, game.getTurnLimit() / 5) ? Theme.ERROR : Theme.INK);
        budget.setMaximum(game.getTurnLimit());
        budget.setValue(game.getTurnsRemaining());
        budget.setForeground(remaining.getForeground() == Theme.ERROR ? Theme.ACCENT : Theme.GREEN);
        budget.getAccessibleContext().setAccessibleName(remaining.getAccessibleContext().getAccessibleName());
        rescued.setText(game.getTeam().getRescuedCount() + " / " + game.getSurvivors().size());
        rescued.getAccessibleContext().setAccessibleName(game.getTeam().getRescuedCount() + " of "
                + game.getSurvivors().size() + " survivors rescued");
        if (renderedGame != game) {
            rebuildRoster(game);
            renderedGame = game;
        }
        for (int index = 0; index < unitButtons.size(); index++) {
            unitButtons.get(index).updateSelection(game.getTeam().getUnits().get(index) == selected);
        }
        board.showGame(game, selected);
        List<Tile> legal = selected == null ? java.util.Collections.emptyList() : selected.getLegalMoves();
        destinations.setText("Legal destinations: " + (legal.isEmpty() ? "none" : legal.stream()
                .map(tile -> "(" + position(tile) + ")").collect(Collectors.joining(" "))));
        destinations.setToolTipText(destinations.getText());
        portrait.setText(selected == null ? "No unit" : unitName(selected));
        portrait.setIcon(selected == null ? null : GameArt.unitIcon(selected, 34));
        detail.setText(selected == null ? "<html>No units registered.<br>Start a new mission to play.</html>" : selectedDetails(selected));
        survivors.setText(survivorDetails(game));
        contextHint.setText(contextHint(game, selected));
        boolean canPickup = active && selected != null && selected.getTile().getSurvivor() != null
                && selected.getCargo().size() < selected.getCapacity();
        boolean canDeliver = active && selected != null && selected.getTile().isShelter() && !selected.getCargo().isEmpty();
        pickup.setEnabled(canPickup);
        deliver.setEnabled(canDeliver);
        Theme.primary(pickup, canPickup);
        Theme.primary(deliver, canDeliver);
        pickup.setIcon(GameArt.icon("pickup", 18, canPickup ? Color.WHITE : Theme.MUTED));
        deliver.setIcon(GameArt.icon("deliver", 18, canDeliver ? Color.WHITE : Theme.MUTED));
        pickup.setToolTipText(canPickup ? "Pick up the waiting survivor (P). Costs 1 action."
                : "Needs a waiting survivor on this tile and a free cargo seat.");
        deliver.setToolTipText(canDeliver ? "Rescue all cargo at this shelter (D). Costs 1 action."
                : "Needs cargo and a shelter tile.");
        feedback.setForeground(controller.isError() ? Theme.ERROR : Theme.INK);
        feedback.setText((controller.isError() ? "Action rejected: " : "") + controller.getMessage());
        feedback.setCaretPosition(0);
        feedbackPanel.setBackground(controller.isError() ? Theme.SOFT_ERROR : Theme.PAPER);
        feedbackIcon.setIcon(GameArt.icon(controller.isError() ? "help" : "check", 21,
                controller.isError() ? Theme.ERROR : Theme.GREEN));
        if (active) {
            ((CardLayout) missionHeader.getLayout()).show(missionHeader, "status");
        } else {
            boolean won = game.getStatus() == MissionStatus.WON;
            resultPanel.setBackground(won ? Theme.SOFT_GREEN : Theme.SOFT_ERROR);
            resultTitle.setText(won ? "Everyone made it home." : "Out of actions. Try a new route.");
            resultTitle.setIcon(GameArt.icon(won ? "check" : "flag", 25, won ? Theme.GREEN : Theme.ERROR));
            resultTitle.setIconTextGap(10);
            resultSummary.setText((won ? "Mission won" : "Mission lost") + " · "
                    + game.getTeam().getRescuedCount() + " of " + game.getSurvivors().size()
                    + " rescued · " + actionCount(game.getTurnsUsed()) + " used · " + game.getTurnsRemaining() + " remaining");
            ((CardLayout) missionHeader.getLayout()).show(missionHeader, "result");
        }
        revalidate();
        repaint();
    }

    private void rebuildRoster(RescueGame game) {
        roster.removeAll();
        unitButtons.clear();
        int index = 1;
        for (RescueUnit unit : game.getTeam().getUnits()) {
            SquadButton button = new SquadButton(unit, index++);
            button.addActionListener(event -> act(() -> controller.selectUnit(unit)));
            unitButtons.add(button);
            roster.add(button);
        }
    }

    private String selectedDetails(RescueUnit unit) {
        String cargo = unit.getCargo().stream().map(Survivor::getId).map(Theme::escape).collect(Collectors.joining(", "));
        String upgrade;
        if (unit instanceof UpgradeableUnit) {
            upgrade = ((UpgradeableUnit) unit).isUpgraded()
                    ? (unit instanceof Scout ? "Active: water access" : "Active: capacity 2")
                    : (unit instanceof Scout ? "First delivery unlocks water" : "First delivery adds a seat");
        } else upgrade = unit instanceof CommandUnit ? "Reach shelter to finish" : "Carries two from the start";
        String movement = unit instanceof Scout || unit instanceof CommandUnit ? "8 directions, including diagonal" : "4 directions, no diagonals";
        return "<html><div style='width:162px'>"
                + "<b>" + unitName(unit) + "</b> at (" + position(unit.getTile()) + ")<br>" + movement
                + "<br><b>Cargo</b> &nbsp; " + unit.getCargo().size() + " / " + unit.getCapacity() + " (" + (cargo.isEmpty() ? "empty" : cargo) + ")"
                + "<br><b>Delivered</b> &nbsp; " + unit.getDeliveredCount()
                + "<br><b>" + (unit instanceof UpgradeableUnit ? "Upgrade" : "Role") + "</b><br>" + upgrade + "</div></html>";
    }

    private String survivorDetails(RescueGame game) {
        StringBuilder html = new StringBuilder("<html><table cellspacing='0' cellpadding='0'>");
        for (Survivor survivor : game.getSurvivors()) {
            html.append("<tr><td width='23' valign='top'><b>").append(Theme.escape(survivor.getId())).append("</b></td><td>");
            if (survivor.isRescued()) html.append("<font color='#387A60'>Rescued</font>");
            else if (survivor.getCarrier() != null) html.append("Aboard ").append(unitName(survivor.getCarrier()));
            else html.append("Waiting at (").append(position(survivor.getTile())).append(")");
            html.append("<br><font size='1'>&nbsp;</font></td></tr>");
        }
        if (game.getSurvivors().isEmpty()) html.append("<tr><td>No survivors registered.</td></tr>");
        return html.append("</table></html>").toString();
    }

    private String contextHint(RescueGame game, RescueUnit unit) {
        String title;
        String message;
        if (game.getStatus() == MissionStatus.WON) {
            title = "Rescue complete";
            message = "Your team brought everyone home. Inspect the map or play again.";
        } else if (game.getStatus() == MissionStatus.LOST) {
            title = "Another route awaits";
            message = "Try combining deliveries and clear the shelter for Command.";
        } else if (unit == null) {
            title = "A fresh start";
            message = "No units are registered. Start a new mission to play.";
        } else if (unit.getTile().getSurvivor() != null && unit.getCargo().size() < unit.getCapacity()) {
            title = "Someone needs a lift";
            message = "Pick up " + Theme.escape(unit.getTile().getSurvivor().getId()) + ", then bring them to shelter.";
        } else if (!unit.getCargo().isEmpty() && unit.getTile().isShelter()) {
            title = "You're at shelter";
            message = "Deliver your cargo. Everyone aboard is rescued in one action.";
        } else if (!unit.getCargo().isEmpty()) {
            title = "Bring them to safety";
            message = "Head to the shelter at (5,2). " + (unit.getCargo().size() < unit.getCapacity()
                    ? "You still have room for another survivor." : "Your cargo is full.");
        } else if (game.getTeam().getRescuedCount() == game.getSurvivors().size() && !game.getSurvivors().isEmpty()) {
            title = "One final step";
            message = unit instanceof CommandUnit ? "Bring Command to the shelter at (5,2) to finish the mission."
                    : "Clear the shelter, then select Command and bring it home.";
        } else if (unit instanceof CommandUnit) {
            title = "Lead the rescue";
            message = "Command cannot carry survivors. Keep the shelter clear for deliveries, then move in to finish.";
        } else if (unit.getTile().isShelter()) {
            title = "Make room for the team";
            message = "Move away from shelter so another unit can deliver. Your next rescue is waiting.";
        } else {
            title = "Find a survivor";
            message = "Choose a green-outlined tile. Move onto a survivor, then pick them up.";
        }
        return "<html><div style='width:162px'><b>" + title + "</b><br>" + message + "</div></html>";
    }

    private void bind(int key, Runnable action) {
        String name = "mission-" + key;
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel vertical() {
        JPanel panel = transparent(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    static String unitName(RescueUnit unit) {
        if (unit instanceof RescueTruck) return "Rescue truck";
        if (unit instanceof CommandUnit) return "Command";
        return unit.getClass().getSimpleName();
    }

    private static String position(Tile tile) { return tile.getRow() + "," + tile.getCol(); }

    private static String actionCount(int count) { return count + (count == 1 ? " action" : " actions"); }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(20, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 40);
    }
    @Override public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() >= 980;
    }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() >= 760;
    }
}
