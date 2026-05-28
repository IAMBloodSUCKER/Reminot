package ru.demo;

import ru.demo.i18n.AppLanguage;
import ru.demo.i18n.Text;
import ru.demo.model.Notification;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

public class ReminderInputPanel extends JPanel {

    private static final DateTimeFormatter LIST_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final JTextField inputField = new JTextField();
    private final DefaultListModel<NotificationListItem> activeRemindersModel = new DefaultListModel<>();
    private final JList<NotificationListItem> activeRemindersList = new JList<>(activeRemindersModel);
    private final Color exitButtonBaseColor = new Color(0xB63D3D);
    private final Color exitButtonFlashColor = new Color(0xFF, 0x7A, 0x7A);

    private final JLabel titleLabel = new JLabel();
    private final JLabel inputLabel = new JLabel();
    private final JTextArea hintArea = new JTextArea();
    private final JLabel activeLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JButton languageRuButton = createActionButton("RU", new Color(0x1E5EA0));
    private final JButton languageEnButton = createActionButton("EN", new Color(0x1E5EA0));
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
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(TerminalPalette.BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalPalette.BANNER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(250, 0));

        stylePanelLabel(titleLabel, TerminalPalette.BANNER);
        stylePanelLabel(inputLabel, TerminalPalette.DIM);
        inputLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));

        inputField.setFont(TerminalPalette.MONO_SMALL);
        inputField.setForeground(TerminalPalette.TEXT);
        inputField.setBackground(new Color(0x08, 0x08, 0x08));
        inputField.setCaretColor(TerminalPalette.TEXT);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalPalette.ACCENT, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        inputField.setAlignmentX(LEFT_ALIGNMENT);
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
        hintArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        hintArea.setAlignmentX(LEFT_ALIGNMENT);

        stylePanelLabel(activeLabel, TerminalPalette.DIM);
        activeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));

        activeRemindersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeRemindersList.setFont(TerminalPalette.MONO_SMALL);
        activeRemindersList.setForeground(TerminalPalette.TEXT);
        activeRemindersList.setBackground(new Color(0x08, 0x08, 0x08));
        activeRemindersList.setSelectionBackground(TerminalPalette.ACCENT);
        activeRemindersList.setSelectionForeground(TerminalPalette.BACKGROUND);
        activeRemindersList.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        activeRemindersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && activeRemindersList.locationToIndex(e.getPoint()) >= 0) {
                    deleteSelectedReminder();
                }
            }
        });

        JScrollPane activeScroll = new JScrollPane(activeRemindersList);
        activeScroll.setAlignmentX(LEFT_ALIGNMENT);
        activeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        activeScroll.setPreferredSize(new Dimension(230, 170));
        activeScroll.setBorder(BorderFactory.createLineBorder(TerminalPalette.ACCENT, 1));
        activeScroll.getViewport().setBackground(new Color(0x08, 0x08, 0x08));
        activeScroll.setBackground(new Color(0x08, 0x08, 0x08));
        activeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleListScrollBar(activeScroll.getVerticalScrollBar());

        JButton cleanConsole = createActionButton(Text.BTN_CLEAN_CONSOLE, new Color(0x2D, 0x84, 0xD8));
        cleanConsole.addActionListener(e -> onClean.run());

        JButton listAll = createActionButton(Text.BTN_LIST_ALL, new Color(0x1E5EA0));
        listAll.addActionListener(e -> onListAll.run());

        JButton deleteReminder = createActionButton(Text.BTN_DELETE_SELECTED, new Color(0xD46A6A));
        deleteReminder.addActionListener(e -> deleteSelectedReminder());

        exitButton = createActionButton(Text.BTN_EXIT, exitButtonBaseColor);
        exitButton.addActionListener(e -> onExit.run());

        stylePanelLabel(languageLabel, TerminalPalette.DIM);
        languageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));

        languageRuButton.addActionListener(e -> selectLanguage(AppLanguage.RU));
        languageEnButton.addActionListener(e -> selectLanguage(AppLanguage.EN));

        JPanel languageButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        languageButtons.setOpaque(false);
        languageButtons.setAlignmentX(LEFT_ALIGNMENT);
        languageButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        languageButtons.add(languageRuButton);
        languageButtons.add(languageEnButton);

        add(titleLabel);
        add(inputLabel);
        add(inputField);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 8)));
        add(addButton);
        add(hintArea);
        add(activeLabel);
        add(activeScroll);
        add(languageLabel);
        add(languageButtons);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 6)));
        add(cleanConsole);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 6)));
        add(listAll);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 6)));
        add(deleteReminder);
        add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        add(exitButton);
        add(javax.swing.Box.createVerticalGlue());

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
        languageLabel.setText(Text.panelLanguageLabel());
        hintArea.setText(Text.panelHint());
        exitButton.setText(Text.BTN_EXIT);
        updateLanguageButtons();
    }

    public void setActiveReminders(List<Notification> reminders) {
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
        button.setForeground(selected ? TerminalPalette.BACKGROUND : TerminalPalette.BACKGROUND);
    }

    private void stylePanelLabel(JLabel label, Color color) {
        label.setForeground(color);
        label.setFont(TerminalPalette.MONO_SMALL);
        label.setAlignmentX(LEFT_ALIGNMENT);
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

        private NotificationListItem(Notification notification) {
            this.id = notification.getId() == null ? -1L : notification.getId();
            String time = notification.getTriggerAt() == null
                    ? "--.-- --:--"
                    : notification.getTriggerAt().format(LIST_TIME);
            this.title = "#" + id + "  " + time + "  " + trimMessage(notification.getMessage());
        }

        @Override
        public String toString() {
            return title;
        }

        private static String trimMessage(String text) {
            if (text == null || text.isBlank()) {
                return "(empty)";
            }
            String value = text.trim();
            if (value.length() <= 18) {
                return value;
            }
            return value.substring(0, 15) + "...";
        }
    }
}
