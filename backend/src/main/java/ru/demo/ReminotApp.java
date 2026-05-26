package ru.demo;

import javax.swing.SwingUtilities;
import java.util.Arrays;

public final class ReminotApp {

    private ReminotApp() {
    }

    public static void main(String[] args) {
        boolean backgroundStart = Arrays.stream(args)
                .anyMatch(arg -> "--background".equalsIgnoreCase(arg) || "--tray".equalsIgnoreCase(arg));
        SwingUtilities.invokeLater(() -> ReminotMainFrame.open(backgroundStart));
    }
}
