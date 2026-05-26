package ru.demo;

import java.awt.Color;
import java.awt.Font;

public final class TerminalPalette {

    public static final Color BACKGROUND = Color.BLACK;
    public static final Color TEXT = new Color(0xD0, 0xD0, 0xD0);
    public static final Color DIM = new Color(0x88, 0x88, 0x88);
    public static final Color ACCENT = new Color(0x3D, 0x9E, 0xFF);
    public static final Color BANNER = new Color(0x2B, 0x8C, 0xFF);
    public static final Color TIMESTAMP = new Color(0x5B, 0xC8, 0xE8);
    public static final Color OK = new Color(0x3D, 0xD0, 0x6E);
    public static final Color TAG = new Color(0x4A, 0xA3, 0xFF);
    public static final Color LINK = new Color(0x6A, 0xB0, 0xFF);

    public static final Font MONO = TerminalFonts.mono(Font.PLAIN, 14);
    public static final Font MONO_SMALL = TerminalFonts.mono(Font.PLAIN, 13);

    private TerminalPalette() {
    }
}
