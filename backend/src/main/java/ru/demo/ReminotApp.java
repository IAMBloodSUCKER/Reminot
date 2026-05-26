package ru.demo;

import javax.swing.SwingUtilities;

public final class ReminotApp {

    private ReminotApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReminotMainFrame::open);
    }
}
