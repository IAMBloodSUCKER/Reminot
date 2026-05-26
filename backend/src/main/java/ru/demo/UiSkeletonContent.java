package ru.demo;

import ru.demo.utils.Checking;

public final class UiSkeletonContent {

    private static final Checking CHECK = new Checking();

    private UiSkeletonContent() {
    }

    public static void fill(TerminalConsole console) {
        String systemUser = System.getProperty("user.name", "unknown");
        String computer = System.getenv("COMPUTERNAME");
        if (computer == null || computer.isBlank()) {
            computer = System.getenv("HOSTNAME");
        }
        if (computer == null || computer.isBlank()) {
            computer = "unknown";
        }

        console.appendDim("by: ");
        console.appendPlain("BloodSUCKER");
        console.newLine();

        console.appendDim("repo: ");
        console.appendLink("https://github.com/IAMBloodSUCKER/Reminot");
        console.newLine();

        console.appendDim("donate (Arbitrum/Base): ");
        console.appendAccent("0xe4a1bf07aa8c2194ab94d72812364968ac5b58e3");
        console.newLine();

        console.appendDim("version: ");
        console.appendPlain(Checking.checkVersion());
        console.newLine();
        console.newLine();

        console.appendTag("[#] ");
        console.appendPlain("ОС: ");
        console.appendAccent(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        console.newLine();

        console.appendTag("[#] ");
        console.appendPlain("Пользователь: ");
        console.appendAccent(systemUser);
        console.newLine();

        console.appendTag("[#] ");
        console.appendPlain("Компьютер: ");
        console.appendAccent(computer);
        console.newLine();
        console.newLine();

        console.appendPlain("Проверка обновлений...");
        console.newLine();

        console.appendOkLine("Версия приложения " + CHECK.checkVersion());
        console.newLine();

        console.appendPlain("Ожидание напоминаний...");
        console.newLine();
    }
}
