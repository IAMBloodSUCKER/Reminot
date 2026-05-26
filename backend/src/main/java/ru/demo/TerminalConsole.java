package ru.demo;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;

public class TerminalConsole extends JTextPane {

    private final SimpleAttributeSet plain = attr(TerminalPalette.TEXT);
    private final SimpleAttributeSet dim = attr(TerminalPalette.DIM);
    private final SimpleAttributeSet accent = attr(TerminalPalette.ACCENT);
    private final SimpleAttributeSet timestamp = attr(TerminalPalette.TIMESTAMP);
    private final SimpleAttributeSet ok = attr(TerminalPalette.OK);
    private final SimpleAttributeSet tag = attr(TerminalPalette.TAG);
    private final SimpleAttributeSet link = attr(TerminalPalette.LINK);

    public TerminalConsole() {
        setEditable(false);
        setOpaque(true);
        setBackground(TerminalPalette.BACKGROUND);
        setForeground(TerminalPalette.TEXT);
        setCaretColor(TerminalPalette.TEXT);
        setFont(TerminalPalette.MONO);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 2, 4, 2));
    }

    public void appendPlain(String text) {
        insert(text, plain);
    }

    public void appendDim(String text) {
        insert(text, dim);
    }

    public void appendAccent(String text) {
        insert(text, accent);
    }

    public void appendTimestamp(String text) {
        insert(text, timestamp);
    }

    public void appendOk(String text) {
        insert(text, ok);
    }

    public void appendLink(String text) {
        insert(text, link);
    }

    public void appendTag(String text) {
        insert(text, tag);
    }

    public void newLine() {
        insert("\n", plain);
    }

    public void appendOkLine(String message) {
        appendOk("OK ");
        appendPlain(message);
        newLine();
    }

    public void clearConsole() {
        setText("");
    }

    private void insert(String text, SimpleAttributeSet style) {
        StyledDocument doc = getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, style);
            setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private static SimpleAttributeSet attr(Color color) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);
        StyleConstants.setFontFamily(set, TerminalPalette.MONO.getFamily());
        StyleConstants.setFontSize(set, TerminalPalette.MONO.getSize());
        return set;
    }
}
