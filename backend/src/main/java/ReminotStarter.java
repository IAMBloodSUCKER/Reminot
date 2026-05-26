import ru.demo.ReminotMainFrame;
import ru.demo.UiSkeletonContent;

import javax.swing.*;

public class ReminotStarter {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReminotMainFrame frame = ReminotMainFrame.open();
            UiSkeletonContent.fill(frame.getConsole());
            frame.getInputPanel().focusInput();
        });
    }
}