package ru.demo.system;

import ru.demo.storage.LocalStoragePaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class SingleInstanceCoordinator implements AutoCloseable {

    private static final int PORT = 38657;
    private static final int SOCKET_TIMEOUT_MS = 900;
    private static final String HOST = "127.0.0.1";
    private static final String LOCK_FILE_NAME = "single-instance.lock";
    private static final String SIGNAL_SHOW = "SHOW";
    private static final String SIGNAL_ACK = "REMINOT_OK";

    private final FileChannel lockChannel;
    private final FileLock lock;
    private final ServerSocket serverSocket;
    private final Thread listenerThread;
    private volatile boolean running = true;

    private SingleInstanceCoordinator(
            FileChannel lockChannel,
            FileLock lock,
            ServerSocket serverSocket,
            Runnable onSignal
    ) {
        this.lockChannel = lockChannel;
        this.lock = lock;
        this.serverSocket = serverSocket;
        if (serverSocket != null) {
            this.listenerThread = new Thread(() -> listen(onSignal), "reminot-single-instance-listener");
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();
        } else {
            this.listenerThread = null;
        }
    }

    public static SingleInstanceCoordinator tryAcquire(Runnable onSignal) {
        Objects.requireNonNull(onSignal, "onSignal");
        FileChannel channel = null;
        FileLock lock = null;
        try {
            Path lockFile = LocalStoragePaths.baseDir().resolve(LOCK_FILE_NAME);
            channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return null;
            }
            ServerSocket socket = null;
            try {
                socket = new ServerSocket(PORT, 50, InetAddress.getByName(HOST));
            } catch (IOException ignored) {
                // Another process may reserve this port; keep single-instance lock as primary guard.
            }
            return new SingleInstanceCoordinator(channel, lock, socket, onSignal);
        } catch (IOException ex) {
            safeRelease(lock);
            safeClose(channel);
            return null;
        }
    }

    public static boolean notifyExistingInstance() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            writer.println(SIGNAL_SHOW);
            writer.flush();
            String response = reader.readLine();
            return SIGNAL_ACK.equals(response);
        } catch (IOException ex) {
            return false;
        }
    }

    private void listen(Runnable onSignal) {
        while (running) {
            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                String command = reader.readLine();
                if (SIGNAL_SHOW.equals(command)) {
                    onSignal.run();
                    writer.println(SIGNAL_ACK);
                    writer.flush();
                }
            } catch (SocketException ex) {
                if (running) {
                    break;
                }
            } catch (IOException ex) {
                if (running) {
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        safeClose(serverSocket);
        safeRelease(lock);
        safeClose(lockChannel);
    }

    private static void safeClose(ServerSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static void safeClose(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    private static void safeRelease(FileLock fileLock) {
        if (fileLock == null) {
            return;
        }
        try {
            fileLock.release();
        } catch (IOException ignored) {
        }
    }
}
