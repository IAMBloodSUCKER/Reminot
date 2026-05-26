package ru.demo;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;

public class BannerPanel extends JPanel {

    private static final int WIDTH = 66;
    private static final int PADDING_H = 14;
    private static final int PADDING_V = 12;
    private static final int BORDER = 1;

    private static final String[] BLOCK_LINES = {
            "╔════════════════════════════════════════════════════════════════╗",
            "║ ██████╗ ███████╗███╗   ███╗██╗███╗   ██╗ ██████╗ ████████╗     ║",
            "║ ██╔══██╗██╔════╝████╗ ████║██║████╗  ██║██╔═══██╗╚══██╔══╝     ║",
            "║ ██████╔╝█████╗  ██╔████╔██║██║██╔██╗ ██║██║   ██║   ██║        ║",
            "║ ██╔══██╗██╔══╝  ██║╚██╔╝██║██║██║╚██╗██║██║   ██║   ██║        ║",
            "║ ██║  ██║███████╗██║ ╚═╝ ██║██║██║ ╚████║╚██████╔╝   ██║        ║",
            "║ ╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝    ╚═╝        ║",
            "╚════════════════════════════════════════════════════════════════╝"
    };

    private static final String FALLBACK_TITLE = "REMINOT";

    private final Font bannerFont;
    private final boolean blockMode;
    private final FontMetrics metrics;

    public BannerPanel() {
        setBackground(TerminalPalette.BACKGROUND);
        setBorder(new LineBorder(TerminalPalette.BANNER, BORDER));
        bannerFont = TerminalFonts.banner(11);
        blockMode = TerminalFonts.supportsBlockBanner(bannerFont);
        metrics = Toolkit.getDefaultToolkit().getFontMetrics(bannerFont);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2.setFont(bannerFont);
            g2.setColor(TerminalPalette.BANNER);

            int x = PADDING_H + BORDER;
            int y = PADDING_V + BORDER + metrics.getAscent();

            if (blockMode) {
                for (String line : BLOCK_LINES) {
                    g2.drawString(pad(line), x, y);
                    y += metrics.getHeight();
                }
            } else {
                Font fallback = TerminalFonts.mono(Font.BOLD, 28);
                g2.setFont(fallback);
                FontMetrics big = g2.getFontMetrics();
                int textX = x + (contentWidth() - big.stringWidth(FALLBACK_TITLE)) / 2;
                int textY = PADDING_V + BORDER + big.getAscent()
                        + (contentHeight() - big.getHeight()) / 2;
                g2.drawString(FALLBACK_TITLE, textX, textY);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        return new Dimension(
                contentWidth() + insets.left + insets.right,
                contentHeight() + insets.top + insets.bottom
        );
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private int contentWidth() {
        if (blockMode) {
            return PADDING_H * 2 + BORDER * 2 + metrics.stringWidth(pad(BLOCK_LINES[0]));
        }
        FontMetrics big = getFontMetrics(TerminalFonts.mono(Font.BOLD, 28));
        return PADDING_H * 2 + BORDER * 2 + Math.max(big.stringWidth(FALLBACK_TITLE), 320);
    }

    private int contentHeight() {
        if (blockMode) {
            return PADDING_V * 2 + BORDER * 2 + metrics.getHeight() * BLOCK_LINES.length;
        }
        return PADDING_V * 2 + BORDER * 2 + 56;
    }

    private static String pad(String line) {
        if (line.length() >= WIDTH) {
            return line.substring(0, WIDTH);
        }
        return line + " ".repeat(WIDTH - line.length());
    }
}
