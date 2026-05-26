package ru.demo;

import ru.demo.model.Notification;
import ru.demo.repository.NotificationRepository;
import ru.demo.service.NotificationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class ReminotMainFrame extends JFrame {

    private static final Dimension START_SIZE = new Dimension(820, 640);
    private static final int RESIZE_MARGIN = 6;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String HEADER_TICKER_MESSAGE =
            "Введите текст напоминания в Input -> нажмите Add -> установите дату и время";
    private final TerminalConsole console = new TerminalConsole();
    private final ReminderInputPanel inputPanel = new ReminderInputPanel();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final NotificationService notificationService;
    private Timer headerTickerTimer;
    private long nextNotificationId = 1L;
    private Point dragStartScreen;
    private Point dragStartWindow;

    public ReminotMainFrame() {
        super("Reminot");
        this.notificationService = new NotificationService(
                notificationRepository,
                this::onNotificationFired
        );

        setUndecorated(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(640, 480));
        setSize(START_SIZE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(TerminalPalette.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(createDragHeader(), BorderLayout.NORTH);
        header.add(new BannerPanel(), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.add(wrapConsole(console), BorderLayout.CENTER);
        body.add(inputPanel, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);
        getContentPane().setBackground(TerminalPalette.BACKGROUND);
        installResizeSupport(root);
        nextNotificationId = computeNextNotificationId();
        UiSkeletonContent.fill(console);

        inputPanel.setOnSubmit(text -> {
            LocalDateTime remindAt = askReminderDateTime(text);
            if (remindAt == null) {
                return false;
            }
            Notification notification = new Notification(
                    nextNotificationId++,
                    remindAt,
                    text,
                    true
            );
            notificationRepository.addNotification(notification);
            refreshActiveReminderList();

            appendLogSection("scheduled reminder");
            console.appendTimestamp("[" + LocalTime.now().format(TIME) + "]");
            console.appendPlain("  queued: ");
            console.appendAccent(text);
            console.appendPlain(" -> ");
            console.appendAccent(remindAt.format(DATE_TIME));
            console.appendPlain(" (id=");
            console.appendAccent(String.valueOf(notification.getId()));
            console.appendPlain(")");
            console.newLine();
            return true;
        });
        inputPanel.setOnClean(() -> {
            console.clearConsole();
            console.appendTimestamp("Консоль обновлена [" + LocalTime.now().format(TIME) + "]\n");
            UiSkeletonContent.fill(console);

            appendLogSection("console");
            console.appendDim("консоль очищена");
            console.newLine();
        });
        inputPanel.setOnListAll(() -> printNotifications(
                notificationRepository.allNotification(),
                "all reminders"
        ));
        inputPanel.setOnExit(this::terminateApplication);
        inputPanel.setOnDeleteById(this::deleteReminderById);
        refreshActiveReminderList();
        notificationService.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                appendLogSection("window");
                console.appendDim("Закрытие через системные кнопки отключено");
                console.newLine();
                console.appendPlain("Нажмите кнопку ");
                console.appendAccent("ЗАВЕРШИТЬ РАБОТУ");
                console.appendPlain(" справа внизу.");
                console.newLine();
                inputPanel.flashExitButton();
            }
        });
    }

    public TerminalConsole getConsole() {
        return console;
    }

    public ReminderInputPanel getInputPanel() {
        return inputPanel;
    }

    public static ReminotMainFrame open() {
        ReminotMainFrame frame = new ReminotMainFrame();
        frame.setVisible(true);
        return frame;
    }

    private LocalDateTime askReminderDateTime(String reminderText) {
        LocalDateTime now = LocalDateTime.now();
        JTextField dateTimeField = new JTextField(now.format(DATE_TIME));
        styleDateTimeField(dateTimeField);
        installDateTimeFilter(dateTimeField);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(TerminalPalette.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel reminderLabel = new JLabel("\"" + shorten(reminderText, 34) + "\"");
        reminderLabel.setFont(TerminalPalette.MONO_SMALL);
        reminderLabel.setForeground(TerminalPalette.TEXT);
        reminderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JLabel title = new JLabel("Когда напомнить:");
        title.setFont(TerminalPalette.MONO_SMALL);
        title.setForeground(TerminalPalette.DIM);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(reminderLabel, BorderLayout.NORTH);
        center.add(title, BorderLayout.CENTER);
        center.add(dateTimeField, BorderLayout.SOUTH);
        content.add(center, BorderLayout.CENTER);

        JButton okButton = createDialogButton("Запланировать", TerminalPalette.OK);
        JButton cancelButton = createDialogButton("Отмена", new java.awt.Color(0x2D, 0x84, 0xD8));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(okButton);

        LocalDateTime[] picked = new LocalDateTime[1];
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new java.awt.Color(0xFF, 0x7A, 0x7A));
        errorLabel.setFont(TerminalPalette.MONO_SMALL);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(errorLabel, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TerminalPalette.BANNER));

        JLabel headerTitle = new JLabel("Время уведомления");
        headerTitle.setFont(TerminalPalette.MONO_SMALL);
        headerTitle.setForeground(TerminalPalette.TEXT);
        headerTitle.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

        JButton closeButton = new JButton("x");
        closeButton.setFont(TerminalPalette.MONO_SMALL);
        closeButton.setForeground(TerminalPalette.TEXT);
        closeButton.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        header.add(headerTitle, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(TerminalPalette.BACKGROUND);
        shell.setBorder(BorderFactory.createLineBorder(TerminalPalette.BANNER, 1));
        shell.add(header, BorderLayout.NORTH);
        shell.add(content, BorderLayout.CENTER);

        JDialog dialog = new JDialog(this, "Время уведомления", true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(shell);
        dialog.getRootPane().setDefaultButton(okButton);

        okButton.addActionListener(e -> {
            try {
                String normalized = normalizeDateTimeInput(dateTimeField.getText());
                picked[0] = LocalDateTime.parse(normalized, DATE_TIME);
                dialog.dispose();
            } catch (DateTimeParseException ex) {
                errorLabel.setText("Формат: дд.мм.гггг чч:мм");
                dateTimeField.requestFocusInWindow();
                dateTimeField.selectAll();
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(() -> {
            dateTimeField.requestFocusInWindow();
            dateTimeField.selectAll();
        });
        dialog.setVisible(true);
        return picked[0];
    }

    private JButton createDialogButton(String text, java.awt.Color background) {
        JButton button = new JButton(text);
        button.setFont(TerminalPalette.MONO_SMALL);
        button.setForeground(TerminalPalette.BACKGROUND);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x0F, 0x2C, 0x4A), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return button;
    }

    private void styleDateTimeField(JTextField textField) {
        textField.setFont(TerminalPalette.MONO_SMALL);
        textField.setForeground(TerminalPalette.TEXT);
        textField.setBackground(new java.awt.Color(0x08, 0x08, 0x08));
        textField.setCaretColor(TerminalPalette.TEXT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalPalette.ACCENT, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private void installDateTimeFilter(JTextField field) {
        javax.swing.text.AbstractDocument doc = (javax.swing.text.AbstractDocument) field.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (isAllowed(string)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (isAllowed(text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            private boolean isAllowed(String value) {
                return value == null || value.matches("[0-9.:\\s]*");
            }
        });
    }

    private String normalizeDateTimeInput(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }

    private void printNotifications(List<Notification> notifications, String title) {
        appendLogSection(title);
        console.appendTimestamp("[" + LocalTime.now().format(TIME) + "]");
        console.appendPlain("  " + title + ": ");
        console.appendAccent(String.valueOf(notifications.size()));
        console.newLine();

        if (notifications.isEmpty()) {
            console.appendDim("  список пуст");
            console.newLine();
            return;
        }

        for (Notification notification : notifications) {
            console.appendTag("  [#] ");
            console.appendPlain("id=");
            console.appendAccent(String.valueOf(notification.getId()));
            console.appendPlain(" time=");
            console.appendAccent(notification.getTriggerAt() == null
                    ? "null"
                    : notification.getTriggerAt().format(DATE_TIME));
            console.appendPlain(" active=");
            console.appendAccent(String.valueOf(notification.isActive()));
            console.appendPlain(" message=");
            console.appendPlain(notification.getMessage());
            console.newLine();
        }
    }

    private void deleteReminderById(long id) {
        Notification notification = notificationRepository.getNotificationById(id);
        if (notification == null) {
            appendLogSection("delete reminder");
            console.appendTimestamp("[" + LocalTime.now().format(TIME) + "]");
            console.appendPlain("  id not found: ");
            console.appendAccent(String.valueOf(id));
            console.newLine();
            return;
        }

        if (!confirmDeleteNotification(notification)) {
            return;
        }

        notificationRepository.deleteNotificationById(id);
        refreshActiveReminderList();
        appendLogSection("delete reminder");
        console.appendTimestamp("[" + LocalTime.now().format(TIME) + "]");
        console.appendPlain("  deleted id=");
        console.appendAccent(String.valueOf(id));
        console.newLine();
    }

    private boolean confirmDeleteNotification(Notification notification) {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(TerminalPalette.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Удалить выбранное напоминание?");
        title.setFont(TerminalPalette.MONO_SMALL);
        title.setForeground(TerminalPalette.DIM);

        JLabel itemLabel = new JLabel(
                "#" + notification.getId() + "  "
                        + (notification.getTriggerAt() == null
                        ? "--.--.---- --:--"
                        : notification.getTriggerAt().format(DATE_TIME)) + "  "
                        + shorten(notification.getMessage(), 24)
        );
        itemLabel.setFont(TerminalPalette.MONO_SMALL);
        itemLabel.setForeground(TerminalPalette.TEXT);
        itemLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(title, BorderLayout.NORTH);
        center.add(itemLabel, BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);

        JButton cancelButton = createDialogButton("Отмена", new java.awt.Color(0x2D, 0x84, 0xD8));
        JButton deleteButton = createDialogButton("Удалить", new java.awt.Color(0xD46A6A));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(cancelButton);
        actions.add(deleteButton);
        content.add(actions, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TerminalPalette.BANNER));

        JLabel headerTitle = new JLabel("Удаление напоминания");
        headerTitle.setFont(TerminalPalette.MONO_SMALL);
        headerTitle.setForeground(TerminalPalette.TEXT);
        headerTitle.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

        JButton closeButton = new JButton("x");
        closeButton.setFont(TerminalPalette.MONO_SMALL);
        closeButton.setForeground(TerminalPalette.TEXT);
        closeButton.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        header.add(headerTitle, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(TerminalPalette.BACKGROUND);
        shell.setBorder(BorderFactory.createLineBorder(TerminalPalette.BANNER, 1));
        shell.add(header, BorderLayout.NORTH);
        shell.add(content, BorderLayout.CENTER);

        JDialog dialog = new JDialog(this, "Удаление напоминания", true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(shell);
        dialog.getRootPane().setDefaultButton(deleteButton);

        final boolean[] confirmed = {false};
        deleteButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return confirmed[0];
    }

    private void refreshActiveReminderList() {
        inputPanel.setActiveReminders(notificationRepository.allActiveNotification());
    }

    private long computeNextNotificationId() {
        long max = 0L;
        for (Notification notification : notificationRepository.allNotification()) {
            if (notification.getId() != null && notification.getId() > max) {
                max = notification.getId();
            }
        }
        return max + 1L;
    }

    private void onNotificationFired(Notification notification) {
        SwingUtilities.invokeLater(() -> {
            refreshActiveReminderList();
            appendLogSection("notification fired");
            console.appendTimestamp("[" + LocalTime.now().format(TIME) + "]");
            console.appendPlain("  fired id=");
            console.appendAccent(String.valueOf(notification.getId()));
            console.appendPlain(" message=");
            console.appendPlain(notification.getMessage());
            console.newLine();
        });
    }

    private void appendLogSection(String title) {
        console.newLine();
        console.appendDim("------------------------------------------------");
        console.newLine();
        console.appendTag("[*] ");
        console.appendAccent(title);
        console.newLine();
    }

    private JPanel createDragHeader() {
        JPanel dragHeader = new JPanel(new BorderLayout());
        dragHeader.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        dragHeader.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, TerminalPalette.BANNER));

        JLabel title = new JLabel("REMINOT");
        title.setForeground(TerminalPalette.DIM);
        title.setFont(TerminalPalette.MONO_SMALL);
        title.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        HeaderTicker ticker = new HeaderTicker(HEADER_TICKER_MESSAGE);
        ticker.setForeground(TerminalPalette.ACCENT);
        ticker.setFont(TerminalPalette.MONO_SMALL);
        ticker.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 10));

        JButton minimizeButton = new JButton("_");
        minimizeButton.setFont(TerminalPalette.MONO_SMALL);
        minimizeButton.setForeground(TerminalPalette.TEXT);
        minimizeButton.setBackground(new java.awt.Color(0x0A, 0x0A, 0x0A));
        minimizeButton.setFocusPainted(false);
        minimizeButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));

        dragHeader.add(title, BorderLayout.WEST);
        dragHeader.add(ticker, BorderLayout.CENTER);
        dragHeader.add(minimizeButton, BorderLayout.EAST);
        installDragSupport(dragHeader);
        installDragSupport(title);
        installDragSupport(ticker);
        startTickerAnimation(ticker);
        return dragHeader;
    }

    private void startTickerAnimation(HeaderTicker ticker) {
        if (headerTickerTimer != null) {
            headerTickerTimer.stop();
        }
        headerTickerTimer = new Timer(25, e -> ticker.advance(1.4));
        headerTickerTimer.start();
    }

    private void installDragSupport(java.awt.Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartScreen = e.getLocationOnScreen();
                dragStartWindow = getLocation();
            }
        });
        component.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartScreen == null || dragStartWindow == null) {
                    return;
                }
                Point current = e.getLocationOnScreen();
                int dx = current.x - dragStartScreen.x;
                int dy = current.y - dragStartScreen.y;
                setLocation(dragStartWindow.x + dx, dragStartWindow.y + dy);
            }
        });
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartScreen = null;
                dragStartWindow = null;
            }
        });
    }

    private void terminateApplication() {
        if (headerTickerTimer != null) {
            headerTickerTimer.stop();
        }
        notificationService.stop();
        dispose();
        System.exit(0);
    }

    private void installResizeSupport(Container root) {
        ResizeMouseHandler resizeHandler = new ResizeMouseHandler();
        installResizeListenersRecursively(root, resizeHandler);
    }

    private void installResizeListenersRecursively(Component component, ResizeMouseHandler handler) {
        component.addMouseListener(handler);
        component.addMouseMotionListener(handler);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installResizeListenersRecursively(child, handler);
            }
        }
    }

    private int resolveResizeDirection(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), getContentPane());
        boolean left = p.x <= RESIZE_MARGIN;
        boolean right = p.x >= getContentPane().getWidth() - RESIZE_MARGIN;
        boolean top = p.y <= RESIZE_MARGIN;
        boolean bottom = p.y >= getContentPane().getHeight() - RESIZE_MARGIN;

        if (left && top) {
            return Cursor.NW_RESIZE_CURSOR;
        }
        if (right && top) {
            return Cursor.NE_RESIZE_CURSOR;
        }
        if (left && bottom) {
            return Cursor.SW_RESIZE_CURSOR;
        }
        if (right && bottom) {
            return Cursor.SE_RESIZE_CURSOR;
        }
        if (left) {
            return Cursor.W_RESIZE_CURSOR;
        }
        if (right) {
            return Cursor.E_RESIZE_CURSOR;
        }
        if (top) {
            return Cursor.N_RESIZE_CURSOR;
        }
        if (bottom) {
            return Cursor.S_RESIZE_CURSOR;
        }
        return Cursor.DEFAULT_CURSOR;
    }

    private final class ResizeMouseHandler extends MouseAdapter {
        private int resizeCursor = Cursor.DEFAULT_CURSOR;
        private Point dragStartScreen;
        private Rectangle dragStartBounds;

        @Override
        public void mouseMoved(MouseEvent e) {
            int direction = resolveResizeDirection(e);
            getContentPane().setCursor(Cursor.getPredefinedCursor(direction));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (resizeCursor == Cursor.DEFAULT_CURSOR) {
                getContentPane().setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            resizeCursor = resolveResizeDirection(e);
            dragStartScreen = e.getLocationOnScreen();
            dragStartBounds = getBounds();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (resizeCursor == Cursor.DEFAULT_CURSOR || dragStartScreen == null || dragStartBounds == null) {
                return;
            }
            Point current = e.getLocationOnScreen();
            int dx = current.x - dragStartScreen.x;
            int dy = current.y - dragStartScreen.y;

            Rectangle newBounds = new Rectangle(dragStartBounds);
            Dimension min = getMinimumSize();

            switch (resizeCursor) {
                case Cursor.N_RESIZE_CURSOR:
                    newBounds.y += dy;
                    newBounds.height -= dy;
                    break;
                case Cursor.S_RESIZE_CURSOR:
                    newBounds.height += dy;
                    break;
                case Cursor.W_RESIZE_CURSOR:
                    newBounds.x += dx;
                    newBounds.width -= dx;
                    break;
                case Cursor.E_RESIZE_CURSOR:
                    newBounds.width += dx;
                    break;
                case Cursor.NW_RESIZE_CURSOR:
                    newBounds.x += dx;
                    newBounds.width -= dx;
                    newBounds.y += dy;
                    newBounds.height -= dy;
                    break;
                case Cursor.NE_RESIZE_CURSOR:
                    newBounds.width += dx;
                    newBounds.y += dy;
                    newBounds.height -= dy;
                    break;
                case Cursor.SW_RESIZE_CURSOR:
                    newBounds.x += dx;
                    newBounds.width -= dx;
                    newBounds.height += dy;
                    break;
                case Cursor.SE_RESIZE_CURSOR:
                    newBounds.width += dx;
                    newBounds.height += dy;
                    break;
                default:
                    return;
            }

            if (newBounds.width < min.width) {
                if (resizeCursor == Cursor.W_RESIZE_CURSOR
                        || resizeCursor == Cursor.NW_RESIZE_CURSOR
                        || resizeCursor == Cursor.SW_RESIZE_CURSOR) {
                    newBounds.x -= (min.width - newBounds.width);
                }
                newBounds.width = min.width;
            }
            if (newBounds.height < min.height) {
                if (resizeCursor == Cursor.N_RESIZE_CURSOR
                        || resizeCursor == Cursor.NW_RESIZE_CURSOR
                        || resizeCursor == Cursor.NE_RESIZE_CURSOR) {
                    newBounds.y -= (min.height - newBounds.height);
                }
                newBounds.height = min.height;
            }
            setBounds(newBounds);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            resizeCursor = Cursor.DEFAULT_CURSOR;
            dragStartScreen = null;
            dragStartBounds = null;
        }
    }

    private static JScrollPane wrapConsole(TerminalConsole console) {
        JScrollPane scroll = new JScrollPane(
                console,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(TerminalPalette.BACKGROUND);
        scroll.setBackground(TerminalPalette.BACKGROUND);
        styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private static void styleScrollBar(javax.swing.JScrollBar bar) {
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new java.awt.Color(0x33, 0x33, 0x33);
                trackColor = java.awt.Color.BLACK;
            }

            @Override
            protected javax.swing.JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected javax.swing.JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private javax.swing.JButton zeroButton() {
                javax.swing.JButton button = new javax.swing.JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setBackground(java.awt.Color.BLACK);
    }

    private static final class HeaderTicker extends JPanel {
        private static final int GAP_PX = 56;
        private final String message;
        private double offsetPx;

        private HeaderTicker(String message) {
            this.message = message;
            setOpaque(false);
        }

        private void advance(double stepPx) {
            int cycleWidth = getMessageWidth() + GAP_PX;
            if (cycleWidth <= 0) {
                return;
            }
            offsetPx += stepPx;
            while (offsetPx >= cycleWidth) {
                offsetPx -= cycleWidth;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(getForeground());

                FontMetrics fm = g2.getFontMetrics();
                int messageWidth = fm.stringWidth(message);
                int cycleWidth = messageWidth + GAP_PX;
                if (messageWidth <= 0 || cycleWidth <= 0 || getWidth() <= 0 || getHeight() <= 0) {
                    return;
                }

                int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                int startX = (int) Math.round(-offsetPx);
                while (startX > 0) {
                    startX -= cycleWidth;
                }
                while (startX < getWidth()) {
                    g2.drawString(message, startX, baseline);
                    startX += cycleWidth;
                }
            } finally {
                g2.dispose();
            }
        }

        private int getMessageWidth() {
            FontMetrics fm = getFontMetrics(getFont());
            return fm == null ? 0 : fm.stringWidth(message);
        }
    }
}
