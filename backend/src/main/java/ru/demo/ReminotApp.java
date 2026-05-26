package ru.demo;

import ru.demo.system.SingleInstanceCoordinator;

import javax.swing.SwingUtilities;
import java.util.Arrays;

public final class ReminotApp {

    private ReminotApp() {
    }

    public static void main(String[] args) {
        boolean backgroundStart = Arrays.stream(args)
                .anyMatch(arg -> "--background".equalsIgnoreCase(arg) || "--tray".equalsIgnoreCase(arg));
        SingleInstanceCoordinator coordinator = SingleInstanceCoordinator.tryAcquire(ReminotMainFrame::activateRunningInstance);
        if (coordinator == null) {
            SingleInstanceCoordinator.notifyExistingInstance();
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(coordinator::close, "reminot-single-instance-shutdown"));
        SwingUtilities.invokeLater(() -> ReminotMainFrame.open(backgroundStart));
    }
}
