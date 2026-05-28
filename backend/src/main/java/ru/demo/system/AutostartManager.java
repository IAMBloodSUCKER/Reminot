package ru.demo.system;

import ru.demo.i18n.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AutostartManager {

    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "Reminot";
    private String lastErrorMessage = "";

    public boolean isSupported() {
        return isWindows();
    }

    public boolean isEnabled() {
        if (!isSupported()) {
            return false;
        }
        CommandResult result = runReg("query", RUN_KEY, "/v", VALUE_NAME);
        return result.exitCode == 0;
    }

    public boolean setEnabled(boolean enabled) {
        lastErrorMessage = "";
        if (!isSupported()) {
            lastErrorMessage = Text.autostartWindowsOnly();
            return false;
        }
        if (enabled) {
            String startupCommand = resolveStartupCommand();
            if (startupCommand == null || startupCommand.isBlank()) {
                lastErrorMessage = Text.autostartExeRequired();
                return false;
            }
            CommandResult result = runReg(
                    "add",
                    RUN_KEY,
                    "/v",
                    VALUE_NAME,
                    "/t",
                    "REG_SZ",
                    "/d",
                    startupCommand,
                    "/f"
            );
            if (result.exitCode != 0) {
                lastErrorMessage = toFriendlyError(result);
            }
            return result.exitCode == 0;
        }
        if (!isEnabled()) {
            return true;
        }
        CommandResult result = runReg("delete", RUN_KEY, "/v", VALUE_NAME, "/f");
        if (result.exitCode != 0) {
            lastErrorMessage = toFriendlyError(result);
        }
        return result.exitCode == 0;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage == null ? "" : lastErrorMessage;
    }

    private String resolveStartupCommand() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            return quote(appPath) + " --background";
        }
        return null;
    }

    private CommandResult runReg(String... args) {
        List<String> command = new ArrayList<>();
        command.add("reg.exe");
        for (String arg : args) {
            command.add(arg);
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            int exit = process.waitFor();
            return new CommandResult(exit, "");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(1, ex.getMessage() == null ? "" : ex.getMessage());
        } catch (IOException ex) {
            return new CommandResult(1, ex.getMessage() == null ? "" : ex.getMessage());
        }
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private String toFriendlyError(CommandResult result) {
        if (result.output != null) {
            String lower = result.output.toLowerCase();
            if (lower.contains("access is denied")) {
                return Text.autostartAccessDenied();
            }
        }
        if (result.exitCode == 5) {
            return Text.autostartAccessDenied();
        }
        return Text.autostartRegistryFailed();
    }

    public static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }
    }
}
