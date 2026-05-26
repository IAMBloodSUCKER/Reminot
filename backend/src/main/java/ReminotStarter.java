import ru.demo.ReminotMainFrame;

import javax.swing.*;

public class ReminotStarter {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReminotMainFrame frame = ReminotMainFrame.open();
            frame.getInputPanel().focusInput();
        });
    }
}