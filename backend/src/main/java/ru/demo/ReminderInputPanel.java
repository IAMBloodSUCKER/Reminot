package ru.demo;

import ru.demo.i18n.AppLanguage;
import ru.demo.i18n.Text;
import ru.demo.model.Notification;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

public class ReminderInputPanel extends JPanel {

    private static final int LIST_MIN_HEIGHT = 100;
    private static final int LIST_PREF_HEIGHT = 140;
    private static final Color LIST_ROW_BG = new Color(0x08, 0x08, 0x08);
    private static final Color LIST_ROW_HOVER_BG = new Color(0x14, 0x2E, 0x48);
    private static final DateTimeFormatter LIST_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private int hoveredReminderIndex = -1;

    private final JTextField inputField = new JTextField();
    private final DefaultListModel<NotificationListItem> activeRemindersModel = new DefaultListModel<>();
    private final JList<NotificationListItem> activeRemindersList = new JList<>(activeRemindersModel) {
        @Override
        public String getToolTipText(MouseEvent event) {
            if (event == null) {
                return null;
            }
            int index = locationToIndex(event.getPoint());
            if (index < 0) {
                return null;
            }
            Rectangle bounds = getCellBounds(index, index);
            if (bounds == null || !bounds.contains(event.getPoint())) {
                return null;
            }
            NotificationListItem item = getModel().getElementAt(index);
            return item == null ? null : item.tooltip;
        }
    };
    private final Color exitButtonBaseColor = new Color(0xB63D3D);
    private final Color exitButtonFlashColor = new Color(0xFF, 0x7A, 0x7A);

    private final JLabel titleLabel = new JLabel();
    private final JLabel inputLabel = new JLabel();
    private final JTextArea hintArea = new JTextArea();
    private final JLabel activeLabel = new JLabel();
    private final JButton languageRuButton = createLanguageButton("RU");
    private final JButton languageEnButton = createLanguageButton("EN");
    private JButton exitButton;

    private Predicate<String> onSubmit = text -> true;
    private Runnable onClean = () -> {
    };
    private Runnable onListAll = () -> {
    };
    private Runnable onExit = () -> {
    };
    private LongConsumer onDeleteById = id -> {
    };
    private Consumer<AppLanguage> onLanguageChange = language -> {
    };

    public ReminderInputPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(TerminalPalette.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalPalette.BANNER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(250, 0));

        stylePanelLabel(titleLabel, TerminalPalette.DIM);
        stylePanelLabel(inputLabel, TerminalPalette.DIM);
        inputLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));

        inputField.setFont(TerminalPalette.MONO_SMALL);
        inputField.setForeground(TerminalPalette.TEXT);
        inputField.setBackground(new Color(0x08, 0x08, 0x08));
        inputField.setCaretColor(TerminalPalette.TEXT);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalPalette.ACCENT, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        inputField.setAlignmentX(LEFT_ALIGNMENT);
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        inputField.addActionListener(e -> submitInput());

        JButton addButton = createActionButton(Text.BTN_ADD, TerminalPalette.ACCENT);
        addButton.addActionListener(e -> submitInput());

        hintArea.setEditable(false);
        hintArea.setFocusable(false);
        hintArea.setOpaque(false);
        hintArea.setLineWrap(true);
        hintArea.setWrapStyleWord(true);
        hintArea.setForeground(TerminalPalette.DIM);
        hintArea.setFont(TerminalPalette.MONO_SMALL);
        hintArea.setAlignmentX(LEFT_ALIGNMENT);
        hintArea.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        stylePanelLabel(activeLabel, TerminalPalette.DIM);
        activeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        activeRemindersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeRemindersList.setFont(TerminalPalette.MONO_SMALL);
        activeRemindersList.setForeground(TerminalPalette.TEXT);
        activeRemindersList.setBackground(new Color(0x08, 0x08, 0x08));
        activeRemindersList.setSelectionBackground(TerminalPalette.ACCENT);
        activeRemindersList.setSelectionForeground(TerminalPalette.BACKGROUND);
        activeRemindersList.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        activeRemindersList.setCellRenderer(new ReminderListCellRenderer());
        MouseAdapter reminderListMouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoveredReminderIndex(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && indexAtPoint(e) >= 0) {
                    deleteSelectedReminder();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredReminderIndex(-1);
            }

            private int indexAtPoint(MouseEvent e) {
                return activeRemindersList.locationToIndex(e.getPoint());
            }

            private void updateHoveredReminderIndex(MouseEvent e) {
                int index = indexAtPoint(e);
                if (index >= 0) {
                    Rectangle bounds = activeRemindersList.getCellBounds(index, index);
                    if (bounds == null || !bounds.contains(e.getPoint())) {
                        index = -1;
                    }
                }
                setHoveredReminderIndex(index);
            }
        };
        activeRemindersList.addMouseListener(reminderListMouseHandler);
        activeRemindersList.addMouseMotionListener(reminderListMouseHandler);

        JScrollPane activeScroll = new JScrollPane(activeRemindersList);
        Dimension listSize = new Dimension(230, LIST_PREF_HEIGHT);
        activeScroll.setPreferredSize(listSize);
        activeScroll.setMinimumSize(new Dimension(0, LIST_MIN_HEIGHT));
        activeScroll.setBorder(BorderFactory.createLineBorder(TerminalPalette.ACCENT, 1));
        activeScroll.getViewport().setBackground(new Color(0x08, 0x08, 0x08));
        activeScroll.setBackground(new Color(0x08, 0x08, 0x08));
        activeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleListScrollBar(activeScroll.getVerticalScrollBar());

        languageRuButton.addActionListener(e -> selectLanguage(AppLanguage.RU));
        languageEnButton.addActionListener(e -> selectLanguage(AppLanguage.EN));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topBar.setOpaque(false);
        topBar.add(titleLabel);
        topBar.add(languageRuButton);
        topBar.add(languageEnButton);

        JPanel inputSection = new JPanel();
        inputSection.setLayout(new BoxLayout(inputSection, BoxLayout.Y_AXIS));
        inputSection.setOpaque(false);
        inputSection.add(inputLabel);
        inputSection.add(inputField);
        inputSection.add(Box.createRigidArea(new Dimension(0, 8)));
        inputSection.add(addButton);
        inputSection.add(hintArea);

        JPanel listSection = new JPanel(new BorderLayout(0, 0));
        listSection.setOpaque(false);
        listSection.add(activeLabel, BorderLayout.NORTH);
        listSection.add(activeScroll, BorderLayout.CENTER);

        JButton cleanConsole = createActionButton(Text.BTN_CLEAN_CONSOLE, new Color(0x2D, 0x84, 0xD8));
        cleanConsole.addActionListener(e -> onClean.run());

        JButton listAll = createActionButton(Text.BTN_LIST_ALL, new Color(0x1E5EA0));
        listAll.addActionListener(e -> onListAll.run());

        JButton deleteReminder = createActionButton(Text.BTN_DELETE_SELECTED, new Color(0xD46A6A));
        deleteReminder.addActionListener(e -> deleteSelectedReminder());

        exitButton = createActionButton(Text.BTN_EXIT, exitButtonBaseColor);
        exitButton.addActionListener(e -> onExit.run());

        JPanel actionsSection = new JPanel();
        actionsSection.setLayout(new BoxLayout(actionsSection, BoxLayout.Y_AXIS));
        actionsSection.setOpaque(false);
        actionsSection.add(cleanConsole);
        actionsSection.add(Box.createRigidArea(new Dimension(0, 6)));
        actionsSection.add(listAll);
        actionsSection.add(Box.createRigidArea(new Dimension(0, 6)));
        actionsSection.add(deleteReminder);
        actionsSection.add(Box.createRigidArea(new Dimension(0, 8)));
        actionsSection.add(exitButton);

        JPanel upperSection = new JPanel(new BorderLayout(0, 8));
        upperSection.setOpaque(false);
        upperSection.add(topBar, BorderLayout.NORTH);
        upperSection.add(inputSection, BorderLayout.CENTER);

        add(upperSection, BorderLayout.NORTH);
        add(listSection, BorderLayout.CENTER);
        add(actionsSection, BorderLayout.SOUTH);

        applyLanguage();
    }

    public void setOnSubmit(Predicate<String> handler) {
        this.onSubmit = Objects.requireNonNullElse(handler, text -> true);
    }

    public void setOnClean(Runnable handler) {
        this.onClean = Objects.requireNonNullElse(handler, () -> {
        });
    }

    public void setOnListAll(Runnable handler) {
        this.onListAll = Objects.requireNonNullElse(handler, () -> {
        });
    }

    public void setOnExit(Runnable handler) {
        this.onExit = Objects.requireNonNullElse(handler, () -> {
        });
    }

    public void setOnDeleteById(LongConsumer handler) {
        this.onDeleteById = Objects.requireNonNullElse(handler, id -> {
        });
    }

    public void setOnLanguageChange(Consumer<AppLanguage> handler) {
        this.onLanguageChange = Objects.requireNonNullElse(handler, language -> {
        });
    }

    public void applyLanguage() {
        titleLabel.setText(Text.panelNewReminderTitle());
        inputLabel.setText(Text.panelInputLabel());
        activeLabel.setText(Text.panelActiveRemindersLabel());
        hintArea.setText(Text.panelHint());
        exitButton.setText(Text.BTN_EXIT);
        updateLanguageButtons();
    }

    public void setActiveReminders(List<Notification> reminders) {
        setHoveredReminderIndex(-1);
        activeRemindersModel.clear();
        if (reminders == null) {
            return;
        }
        for (Notification reminder : reminders) {
            activeRemindersModel.addElement(new NotificationListItem(reminder));
        }
    }

    public void focusInput() {
        inputField.requestFocusInWindow();
    }

    public void flashExitButton() {
        if (exitButton == null) {
            return;
        }
        final int[] ticks = {0};
        Timer timer = new Timer(130, e -> {
            boolean odd = ticks[0] % 2 == 1;
            exitButton.setBackground(odd ? exitButtonFlashColor : exitButtonBaseColor);
            ticks[0]++;
            if (ticks[0] >= 8) {
                exitButton.setBackground(exitButtonBaseColor);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setInitialDelay(0);
        timer.start();
    }

    private void selectLanguage(AppLanguage language) {
        if (Text.language() == language) {
            return;
        }
        Text.setLanguage(language);
        applyLanguage();
        onLanguageChange.accept(language);
    }

    private void updateLanguageButtons() {
        AppLanguage current = Text.language();
        styleLanguageButton(languageRuButton, current == AppLanguage.RU);
        styleLanguageButton(languageEnButton, current == AppLanguage.EN);
    }

    private void styleLanguageButton(JButton button, boolean selected) {
        button.setBackground(selected ? TerminalPalette.ACCENT : new Color(0x1E5EA0));
        button.setForeground(TerminalPalette.BACKGROUND);
    }

    private void stylePanelLabel(JLabel label, Color color) {
        label.setForeground(color);
        label.setFont(TerminalPalette.MONO_SMALL);
        label.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JButton createLanguageButton(String text) {
        JButton button = new JButton(text);
        button.setFont(TerminalPalette.MONO_SMALL);
        button.setForeground(TerminalPalette.BACKGROUND);
        button.setBackground(new Color(0x1E5EA0));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x0F, 0x2C, 0x4A), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        Dimension size = new Dimension(44, 28);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private JButton createActionButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(TerminalPalette.MONO_SMALL);
        button.setForeground(TerminalPalette.BACKGROUND);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x0F, 0x2C, 0x4A), 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return button;
    }

    private void submitInput() {
        String raw = inputField.getText();
        if (raw == null) {
            return;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return;
        }
        boolean accepted = onSubmit.test(text);
        if (accepted) {
            inputField.setText("");
            inputField.requestFocusInWindow();
        }
    }

    private void deleteSelectedReminder() {
        NotificationListItem selected = activeRemindersList.getSelectedValue();
        if (selected == null || selected.id < 0) {
            return;
        }
        onDeleteById.accept(selected.id);
    }

    private void setHoveredReminderIndex(int index) {
        if (hoveredReminderIndex == index) {
            return;
        }
        hoveredReminderIndex = index;
        activeRemindersList.repaint();
    }

    private final class ReminderListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );
            label.setFont(TerminalPalette.MONO_SMALL);
            label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            if (isSelected) {
                label.setBackground(TerminalPalette.ACCENT);
                label.setForeground(TerminalPalette.BACKGROUND);
            } else if (index == hoveredReminderIndex) {
                label.setBackground(LIST_ROW_HOVER_BG);
                label.setForeground(TerminalPalette.TEXT);
            } else {
                label.setBackground(LIST_ROW_BG);
                label.setForeground(TerminalPalette.TEXT);
            }
            label.setOpaque(true);
            if (value instanceof NotificationListItem item) {
                label.setToolTipText(item.tooltip);
            } else {
                label.setToolTipText(null);
            }
            return label;
        }
    }

    private void styleListScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(0x33, 0x33, 0x33);
                trackColor = Color.BLACK;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setBackground(Color.BLACK);
    }

    private static final class NotificationListItem {
        private final long id;
        private final String title;
        private final String tooltip;

        private NotificationListItem(Notification notification) {
            this.id = notification.getId() == null ? -1L : notification.getId();
            String time = notification.getTriggerAt() == null
                    ? "--.-- --:--"
                    : notification.getTriggerAt().format(LIST_TIME);
            String fullMessage = fullMessage(notification.getMessage());
            this.title = "#" + id + "  " + time + "  " + trimMessage(fullMessage);
            this.tooltip = buildTooltip(id, time, fullMessage);
        }

        @Override
        public String toString() {
            return title;
        }

        private static String fullMessage(String text) {
            if (text == null || text.isBlank()) {
                return "(empty)";
            }
            return text.trim();
        }

        private static String trimMessage(String text) {
            if (text.length() <= 18) {
                return text;
            }
            return text.substring(0, 15) + "...";
        }

        private static String buildTooltip(long id, String time, String message) {
            return "<html><body style='font-family:monospace;font-size:11px;max-width:260px'>"
                    + "#" + id + "&nbsp;&nbsp;" + escapeHtml(time)
                    + "<br>" + escapeHtml(message)
                    + "</body></html>";
        }

        private static String escapeHtml(String text) {
            return text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }
}
