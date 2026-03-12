package com.etmisthefox.solacecore.websocket;

import com.etmisthefox.solacecore.managers.LanguageManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModeratorWebSocketServer extends WebSocketServer {

    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final Gson gson = new Gson();
    private final List<WebSocket> connections = new CopyOnWriteArrayList<>();
    private final ModCommandHandler commandHandler;

    public ModeratorWebSocketServer(int port, JavaPlugin plugin, ModCommandHandler commandHandler, LanguageManager lang) {
        super(new InetSocketAddress(port));
        this.plugin = plugin;
        this.commandHandler = commandHandler;
        this.lang = lang;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String clientAddr = conn.getRemoteSocketAddress().toString();
        plugin.getLogger().info("✓ Web client CONNECTED: " + clientAddr);
        plugin.getLogger().info("  Total connections: " + connections.size());
        
        // Potvrzení připojení
        JsonObject response = new JsonObject();
        response.addProperty("type", "connected");
        response.addProperty("message", lang.getMessage("websocket.connected"));
        response.addProperty("version", "1.0");
        response.addProperty("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(response));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        plugin.getLogger().info("✗ Web client DISCONNECTED: " + conn.getRemoteSocketAddress() + " (Code: " + code + ")");
        plugin.getLogger().info("  Total connections: " + connections.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getLogger().info("📨 WebSocket Message Received:");
            plugin.getLogger().info("   From: " + conn.getRemoteSocketAddress());
            plugin.getLogger().info("   Payload: " + message);
            
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String action = json.has("action") ? json.get("action").getAsString() : null;

            if (action == null) {
                plugin.getLogger().warning("❌ Missing action field in message");
                sendError(conn, lang.getMessage("websocket.error.missing_action"));
                return;
            }

            plugin.getLogger().info("   Action: " + action);
            if (json.has("playerName")) {
                plugin.getLogger().info("   Player: " + json.get("playerName").getAsString());
            }
            if (json.has("reason")) {
                plugin.getLogger().info("   Reason: " + json.get("reason").getAsString());
            }

            // Zpracovat příkaz
            commandHandler.handleCommand(action, json, conn, this);
            
            plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
            sendError(conn, lang.getMessage("websocket.error.processing_request", "error", e.getMessage()));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        plugin.getLogger().severe("❌ WebSocket ERROR: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        plugin.getLogger().info("✓✓✓ Moderator WebSocket Server RUNNING on port " + getPort() + " ✓✓✓");
        plugin.getLogger().info("   Web će se připojovat na: ws://localhost:" + getPort());
    }

    public void sendToAll(JsonObject message) {
        String json = gson.toJson(message);
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send(json);
            }
        }
    }

    public void sendToClient(WebSocket conn, JsonObject message) {
        if (conn != null && conn.isOpen()) {
            conn.send(gson.toJson(message));
        }
    }

    public void sendError(WebSocket conn, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("message", error);
        sendToClient(conn, response);
    }

    public void sendSuccess(WebSocket conn, String action, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "success");
        response.addProperty("action", action);
        response.addProperty("message", message);
        sendToClient(conn, response);
    }

    public List<WebSocket> getConnections() {
        return new ArrayList<>(connections);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
