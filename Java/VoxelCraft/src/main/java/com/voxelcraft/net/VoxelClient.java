package com.voxelcraft.net;

import com.voxelcraft.mode.GameMode;
import com.voxelcraft.save.WorldSave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VoxelClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Queue<NetworkMessage> inbox = new ConcurrentLinkedQueue<>();
    private final WorldSave initialSave;
    private volatile boolean running = true;

    public VoxelClient(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            NetworkMessage init = NetworkMessage.decode(in.readLine());
            initialSave = parseInit(init.payload());
            Thread reader = new Thread(this::readLoop, "voxelcraft-lan-reader");
            reader.setDaemon(true);
            reader.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to LAN server " + host + ":" + port, e);
        }
    }

    public WorldSave initialSave() {
        return initialSave;
    }

    public NetworkMessage poll() {
        return inbox.poll();
    }

    public void sendBlockChange(String blockKey, String typeId) {
        send(new NetworkMessage(NetworkMessage.BLOCK, blockKey + "=" + typeId));
    }

    private void send(NetworkMessage message) {
        out.println(message.encode());
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                inbox.add(NetworkMessage.decode(line));
            }
        } catch (IOException ignored) {
        }
    }

    private WorldSave parseInit(String payload) {
        String[] parts = payload.split(";");
        WorldSave save = new WorldSave("lan-client", parts.length > 0 ? GameMode.fromId(parts[0]) : GameMode.FREE,
            parts.length > 1 ? parseInt(parts[1], 12345) : 12345);
        for (int i = 2; i < parts.length; i++) {
            int split = parts[i].indexOf('=');
            if (split > 0) {
                save.modifications.put(parts[i].substring(0, split), parts[i].substring(split + 1));
            }
        }
        return save;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
