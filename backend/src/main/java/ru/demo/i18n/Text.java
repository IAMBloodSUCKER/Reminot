package ru.demo.i18n;

public final class Text {

    public static final String BTN_ADD = "Add";
    public static final String BTN_CLEAN_CONSOLE = "Clean console";
    public static final String BTN_LIST_ALL = "List all";
    public static final String BTN_DELETE_SELECTED = "Delete selected";
    public static final String BTN_EXIT = "QUIT";
    public static final String BTN_SCHEDULE = "Schedule";
    public static final String BTN_CANCEL = "Cancel";
    public static final String BTN_DELETE = "Delete";

    private static volatile AppLanguage language = LanguageStore.load();

    private Text() {
    }

    public static AppLanguage language() {
        return language;
    }

    public static void setLanguage(AppLanguage value) {
        language = value == null ? AppLanguage.RU : value;
        LanguageStore.save(language);
    }

    public static String panelNewReminderTitle() {
        return language == AppLanguage.EN ? "NEW REMINDER" : "НОВОЕ НАПОМИНАНИЕ";
    }

    public static String panelInputLabel() {
        return language == AppLanguage.EN ? "Input:" : "Текст:";
    }

    public static String panelActiveRemindersLabel() {
        return language == AppLanguage.EN ? "Active reminders:" : "Активные напоминания:";
    }

    public static String panelLanguageLabel() {
        return language == AppLanguage.EN ? "Language:" : "Язык:";
    }

    public static String panelHint() {
        if (language == AppLanguage.EN) {
            return "Example:\nCall the manager back\nTurn off the kettle";
        }
        return "Пример:\nПерезвонить начальнику\nВыключить чайник";
    }

    public static String headerTicker() {
        if (language == AppLanguage.EN) {
            return "Type reminder text in Input -> press Add -> set date and time";
        }
        return "Введите текст напоминания в Input -> нажмите Add -> установите дату и время";
    }

    public static String osLabel() {
        return language == AppLanguage.EN ? "OS: " : "ОС: ";
    }

    public static String userLabel() {
        return language == AppLanguage.EN ? "User: " : "Пользователь: ";
    }

    public static String computerLabel() {
        return language == AppLanguage.EN ? "Computer: " : "Компьютер: ";
    }

    public static String checkingUpdates() {
        return language == AppLanguage.EN ? "Checking for updates..." : "Проверка обновлений...";
    }

    public static String appVersionLine(String version) {
        if (language == AppLanguage.EN) {
            return "Application version " + version;
        }
        return "Версия приложения " + version;
    }

    public static String waitingForReminders() {
        return language == AppLanguage.EN ? "Waiting for reminders..." : "Ожидание напоминаний...";
    }

    public static String consoleRefreshed(String time) {
        if (language == AppLanguage.EN) {
            return "Console refreshed [" + time + "]\n";
        }
        return "Консоль обновлена [" + time + "]\n";
    }

    public static String consoleCleared() {
        return language == AppLanguage.EN ? "console cleared" : "консоль очищена";
    }

    public static String windowMinimizedToTray() {
        return language == AppLanguage.EN
                ? "Window minimized to tray."
                : "Окно свернуто в трей.";
    }

    public static String openFromTrayHint() {
        return language == AppLanguage.EN
                ? "To open the app, click the Reminot icon in the tray."
                : "Чтобы открыть приложение, кликните по иконке Reminot в трее.";
    }

    public static String systemCloseDisabled() {
        return language == AppLanguage.EN
                ? "Closing via system buttons is disabled"
                : "Закрытие через системные кнопки отключено";
    }

    public static String pressExitButtonPrefix() {
        return language == AppLanguage.EN ? "Press the " : "Нажмите кнопку ";
    }

    public static String pressExitButtonSuffix() {
        return language == AppLanguage.EN ? " button at the bottom right." : " справа внизу.";
    }

    public static String startedInBackgroundTray() {
        return language == AppLanguage.EN
                ? "Application started in background (tray)."
                : "Приложение запущено в фоне (трей).";
    }

    public static String whenToRemind() {
        return language == AppLanguage.EN ? "When to remind:" : "Когда напомнить:";
    }

    public static String notificationTimeTitle() {
        return language == AppLanguage.EN ? "Notification time" : "Время уведомления";
    }

    public static String dateTimeFormatError() {
        return language == AppLanguage.EN
                ? "Format: dd.MM.yyyy HH:mm"
                : "Формат: дд.мм.гггг чч:мм";
    }

    public static String listEmpty() {
        return language == AppLanguage.EN ? "  list is empty" : "  список пуст";
    }

    public static String deleteSelectedQuestion() {
        return language == AppLanguage.EN
                ? "Delete selected reminder?"
                : "Удалить выбранное напоминание?";
    }

    public static String deleteReminderTitle() {
        return language == AppLanguage.EN ? "Delete reminder" : "Удаление напоминания";
    }

    public static String autostartEnabledDefault() {
        return language == AppLanguage.EN
                ? "Background mode on Windows startup enabled by default."
                : "Фоновый режим при старте Windows включен по умолчанию.";
    }

    public static String autostartEnableFailed() {
        return language == AppLanguage.EN
                ? "Failed to enable background mode on Windows startup."
                : "Не удалось включить фоновый режим при старте Windows.";
    }

    public static String reasonPrefix() {
        return language == AppLanguage.EN ? "Reason: " : "Причина: ";
    }

    public static String unknownTime() {
        return language == AppLanguage.EN ? "unknown time" : "неизвестное время";
    }

    public static String noText() {
        return language == AppLanguage.EN ? "(no text)" : "(без текста)";
    }

    public static String reminderAtPrefix() {
        return language == AppLanguage.EN ? "Reminder at " : "Напоминание на ";
    }

    public static String openedFromTray() {
        return language == AppLanguage.EN ? "Window opened from tray." : "Окно открыто из трея.";
    }

    public static String logScheduledReminder() {
        return language == AppLanguage.EN ? "scheduled reminder" : "напоминание запланировано";
    }

    public static String logConsole() {
        return language == AppLanguage.EN ? "console" : "консоль";
    }

    public static String logActiveReminders() {
        return language == AppLanguage.EN ? "active reminders" : "активные напоминания";
    }

    public static String logDeleteReminder() {
        return language == AppLanguage.EN ? "delete reminder" : "удаление напоминания";
    }

    public static String logNotificationFired() {
        return language == AppLanguage.EN ? "notification fired" : "уведомление сработало";
    }

    public static String logAutostart() {
        return language == AppLanguage.EN ? "autostart" : "автозапуск";
    }

    public static String logTray() {
        return language == AppLanguage.EN ? "tray" : "трей";
    }

    public static String logWindow() {
        return language == AppLanguage.EN ? "window" : "окно";
    }

    public static String logStartup() {
        return language == AppLanguage.EN ? "startup" : "запуск";
    }

    public static String autostartWindowsOnly() {
        return language == AppLanguage.EN
                ? "Autostart is supported only on Windows."
                : "Автозапуск поддерживается только на Windows.";
    }

    public static String autostartAccessDenied() {
        return language == AppLanguage.EN
                ? "No permission to change autostart for the current user."
                : "Нет прав на изменение автозапуска для текущего пользователя.";
    }

    public static String autostartExeRequired() {
        return language == AppLanguage.EN
                ? "This feature is available in the installed EXE version."
                : "Эта функция доступна в установленной EXE-версии приложения.";
    }

    public static String autostartRegistryFailed() {
        return language == AppLanguage.EN
                ? "Failed to write autostart setting to the Windows registry."
                : "Не удалось записать параметр автозапуска в реестр Windows.";
    }
}
