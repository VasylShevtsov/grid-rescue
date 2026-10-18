package edu.whitman.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Presentation tokens shared by the map, controls, and mission panels. */
final class Theme {
    static final Color INK = new Color(0x243F36);
    static final Color MUTED = new Color(0x65736B);
    static final Color BACKGROUND = new Color(0xF5F3EB);
    static final Color PAPER = new Color(0xFFFEFA);
    static final Color LINE = new Color(0xDDDFD4);
    static final Color GREEN = new Color(0x387A60);
    static final Color SOFT_GREEN = new Color(0xE7EEDF);
    static final Color ACCENT = new Color(0xD77A5E);
    static final Color ERROR = new Color(0x9F432C);
    static final Color SOFT_ERROR = new Color(0xF9E9DF);
    static final Color GOLD = new Color(0xE6BD6C);
    private static final Font BASE = loadFont("Regular", Font.PLAIN);
    private static final Font EMPHASIS = loadFont("SemiBold", Font.BOLD);

    private Theme() { }

    private static Font loadFont(String weight, int fallbackStyle) {
        try (InputStream stream = Theme.class.getResourceAsStream("fonts/Outfit-" + weight + ".ttf")) {
            if (stream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
                // Swing's HTML renderer resolves families by name rather than retaining this Font.
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font;
            }
        } catch (java.io.IOException | java.awt.FontFormatException exception) {
            // Standard Java fonts keep the game usable if resources are damaged.
        }
        return new Font(Font.SANS_SERIF, fallbackStyle, 14);
    }

    static Font font(int size, boolean bold) {
        return (bold ? EMPHASIS : BASE).deriveFont((float) size);
    }

    static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(INK);
        return label;
    }

    static JLabel muted(String text, int size) {
        JLabel label = label(text, size, false);
        label.setForeground(MUTED);
        return label;
    }

    static JPanel panel(java.awt.LayoutManager layout, Color color) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(color);
        return panel;
    }

    static JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new RoundedPanel(layout);
        panel.setBackground(PAPER);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    static JButton button(String text) {
        JButton button = new SoftButton(text);
        button.setFont(font(13, true));
        button.setForeground(INK);
        button.setBackground(PAPER);
        button.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
        button.setIconTextGap(8);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    static void primary(JButton button, boolean primary) {
        button.setBackground(primary ? GREEN : PAPER);
        button.setForeground(primary ? Color.WHITE : INK);
        button.putClientProperty("primary", primary);
        button.repaint();
    }

    static void styleScrollPane(JScrollPane pane) {
        pane.setBackground(BACKGROUND);
        pane.getViewport().setBackground(BACKGROUND);
        for (JScrollBar bar : new JScrollBar[] {pane.getVerticalScrollBar(), pane.getHorizontalScrollBar()}) {
            bar.setUI(new QuietScrollBarUI());
            bar.setBackground(BACKGROUND);
            bar.setForeground(MUTED);
            bar.setBorder(BorderFactory.createEmptyBorder());
        }
        pane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, panel(null, BACKGROUND));
    }

    static void quality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Keep Swing's standard scroll model, wheel, keyboard and accessible behavior. */
    private static final class QuietScrollBarUI extends BasicScrollBarUI {
        @Override public Dimension getPreferredSize(JComponent component) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(12, 48) : new Dimension(48, 12);
        }

        @Override protected Dimension getMinimumThumbSize() {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL
                    ? new Dimension(12, 28) : new Dimension(28, 12);
        }

        @Override protected JButton createDecreaseButton(int orientation) { return hiddenArrow(); }
        @Override protected JButton createIncreaseButton(int orientation) { return hiddenArrow(); }

        private JButton hiddenArrow() {
            JButton button = new JButton();
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            graphics.setColor(BACKGROUND);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (!scrollbar.isEnabled() || bounds.isEmpty()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(isDragging || isThumbRollover() ? GREEN : MUTED);
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            g.fillRoundRect(bounds.x + (vertical ? 3 : 2), bounds.y + (vertical ? 2 : 3),
                    Math.max(2, bounds.width - (vertical ? 6 : 4)),
                    Math.max(2, bounds.height - (vertical ? 4 : 6)), 8, 8);
            g.dispose();
        }
    }

    private static final class RoundedPanel extends JPanel {
        RoundedPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g.setColor(LINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class SoftButton extends JButton {
        SoftButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
            getActionMap().put("activate", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    doClick(0);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color fill = isEnabled() ? getBackground() : new Color(0xECEDE6);
            if (isEnabled() && getModel().isPressed()) fill = fill.darker();
            else if (isEnabled() && getModel().isRollover()) {
                fill = Boolean.TRUE.equals(getClientProperty("primary")) ? new Color(0x2E6951) : SOFT_GREEN;
            }
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            g.setColor(Boolean.TRUE.equals(getClientProperty("primary")) && isEnabled() ? GREEN : LINE);
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            if (hasFocus()) {
                g.setColor(GREEN);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 10, 10);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
