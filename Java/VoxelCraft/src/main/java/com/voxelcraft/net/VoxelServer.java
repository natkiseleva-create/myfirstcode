package com.voxelcraft.net;

import com.voxelcraft.mode.GameMode;
import com.voxelcraft.save.WorldSave;
import com.voxelcraft.save.WorldStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VoxelServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 25565;

    private final int port;
    private final WorldSave save;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final CountDownLatch started = new CountDownLatch(1);

    public VoxelServer(int port, String worldName) {
        this.port = port;
        WorldSave loaded = WorldStorage.load(worldName);
        this.save = loaded != null ? loaded : new WorldSave(worldName, GameMode.FREE, 12345);
    }

    public void start() {
        if (running) return;
        running = true;
        acceptThread = new Thread(this::acceptLoop, "voxelcraft-lan-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
        try {
            started.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void acceptLoop() {
        try (ServerSocket socket = new ServerSocket(port)) {
            serverSocket = socket;
            started.countDown();
            while (running) {
                Socket client = socket.accept();
                ClientHandler handler = new ClientHandler(client);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            started.countDown();
            if (running) {
                System.err.println("[LAN] Server stopped: " + e.getMessage());
            }
        }
    }

    private synchronized void applyBlockChange(String payload) {
        int split = payload.indexOf('=');
        if (split < 0) return;
        String key = payload.substring(0, split);
        String type = payload.substring(split + 1);
        save.modifications.put(key, type);
        WorldStorage.save(save);
        broadcast(new NetworkMessage(NetworkMessage.BLOCK, payload));
    }

    private void broadcast(NetworkMessage message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    private String initPayload() {
        StringBuilder payload = new StringBuilder();
        payload.append(save.mode.id).append(';').append(save.seed);
        for (var entry : save.modifications.entrySet()) {
            payload.append(';').append(entry.getKey()).append('=').append(entry.getValue());
        }
        return payload.toString();
    }

    @Override
    public void close() {
        running = false;
        for (ClientHandler client : clients) {
            client.close();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private class ClientHandler implements AutoCloseable {
        private final Socket socket;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        void start() {
            Thread thread = new Thread(this::run, "voxelcraft-lan-client");
            thread.setDaemon(true);
            thread.start();
        }

        private void run() {
            try (BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                out = writer;
                send(new NetworkMessage(NetworkMessage.INIT, initPayload()));
                String line;
                while ((line = in.readLine()) != null) {
                    NetworkMessage message = NetworkMessage.decode(line);
                    if (NetworkMessage.BLOCK.equals(message.type())) {
                        applyBlockChange(message.payload());
                    }
                }
            } catch (IOException ignored) {
            } finally {
                clients.remove(this);
                close();
            }
        }

        void send(NetworkMessage message) {
            if (out != null) {
                out.println(message.encode());
            }
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
