package ru.demo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

public final class TerminalFonts {

    private static final char[] BANNER_GLYPHS = {
            '\u2588', '\u2550', '\u2551', '\u2554', '\u2557', '\u255A', '\u255D', '\u2560', '\u2563'
    };

    private static final String[] MONO_FAMILIES = {
            "Cascadia Mono", "Cascadia Code", "JetBrains Mono",
            "Consolas", "Lucida Console", "Courier New"
    };

    private static final String[] BANNER_FAMILIES = {
            "Cascadia Mono", "Cascadia Code", "JetBrains Mono",
            "Consolas", "Lucida Console", "Segoe UI Symbol"
    };

    private TerminalFonts() {
    }

    public static Font mono(int style, int size) {
        return firstAvailable(MONO_FAMILIES, style, size, false);
    }

    public static Font banner(int size) {
        Font font = firstAvailable(BANNER_FAMILIES, Font.PLAIN, size, true);
        if (font != null) {
            return font;
        }
        return mono(Font.PLAIN, size);
    }

    public static boolean supportsBlockBanner(Font font) {
        if (font == null) {
            return false;
        }
        for (char glyph : BANNER_GLYPHS) {
            if (!font.canDisplay(glyph)) {
                return false;
            }
        }
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
        int blockWidth = metrics.charWidth('\u2588');
        int boxWidth = metrics.charWidth('\u2551');
        return blockWidth > 0 && blockWidth == boxWidth;
    }

    private static Font firstAvailable(String[] families, int style, int size, boolean requireBannerGlyphs) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = env.getAvailableFontFamilyNames();
        for (String preferred : families) {
            for (String name : available) {
                if (!name.equalsIgnoreCase(preferred)) {
                    continue;
                }
                Font font = new Font(name, style, size);
                if (!requireBannerGlyphs || supportsBlockBanner(font)) {
                    return font;
                }
            }
        }
        return new Font(Font.MONOSPACED, style, size);
    }
}
