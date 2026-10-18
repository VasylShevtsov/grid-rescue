package edu.whitman.ui;

import edu.whitman.CommandUnit;
import edu.whitman.Medic;
import edu.whitman.RescueTruck;
import edu.whitman.RescueUnit;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/** Locally bundled Kenney pixel art and Lucide icons; play never requires a network. */
final class GameArt {
    private static final String ROOT = "/edu/whitman/ui/art/";
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static final Map<String, Icon> ICONS = new HashMap<>();

    private GameArt() { }

    static Icon icon(String name, int size) {
        return icon(name, size, Theme.INK);
    }

    static Icon icon(String name, int size, Color color) {
        String key = name + "/" + size + "/" + color.getRGB();
        return ICONS.computeIfAbsent(key, ignored -> {
            BufferedImage source = image("lucide/" + name);
            BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int alpha = source.getRGB(x, y) >>> 24;
                    tinted.setRGB(x, y, (alpha << 24) | (color.getRGB() & 0xFFFFFF));
                }
            }
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(tinted, 0, 0, size, size, null);
            g.dispose();
            return new ImageIcon(scaled);
        });
    }

    static Icon unitIcon(RescueUnit unit, int size) {
        final Icon glyph = icon(unitKey(unit), Math.max(14, (int) (size * .57)));
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(unitColor(unit));
                g.fillOval(x, y, size, size);
                glyph.paintIcon(component, g, x + (size - glyph.getIconWidth()) / 2,
                        y + (size - glyph.getIconHeight()) / 2);
                g.dispose();
            }
        };
    }

    static String unitKey(RescueUnit unit) {
        if (unit instanceof RescueTruck) return "truck";
        if (unit instanceof Medic) return "medic";
        if (unit instanceof CommandUnit) return "command";
        return "scout";
    }

    static String unitLabel(RescueUnit unit) {
        if (unit instanceof RescueTruck) return "Truck";
        if (unit instanceof Medic) return "Medic";
        if (unit instanceof CommandUnit) return "Cmd";
        return "Scout";
    }

    static Color unitColor(RescueUnit unit) {
        if (unit instanceof RescueTruck) return new Color(0xECD5A0);
        if (unit instanceof Medic) return new Color(0xF0C4B4);
        if (unit instanceof CommandUnit) return new Color(0xCDD5E7);
        return new Color(0xC1DCC7);
    }

    static BufferedImage image(String name) {
        return IMAGES.computeIfAbsent(name, key -> {
            try (InputStream stream = GameArt.class.getResourceAsStream(ROOT + key + ".png")) {
                if (stream == null) throw new IllegalStateException("Missing bundled artwork: " + key);
                BufferedImage result = ImageIO.read(stream);
                if (result == null) throw new IllegalStateException("Unreadable bundled artwork: " + key);
                return result;
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot read bundled artwork: " + key, ex);
            }
        });
    }

    static void sprite(Graphics2D g, String name, int x, int y, int size) {
        Object previous = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image(name), x, y, size, size, null);
        if (previous != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
    }
}
