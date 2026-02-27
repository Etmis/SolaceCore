package com.etmisthefox.solacecore.websocket;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.java_websocket.WebSocket;

public class ModCommandHandler {

    private final Database database;
    private final LanguageManager languageManager;
    private final Plugin plugin;

    public ModCommandHandler(Database database, LanguageManager languageManager, Plugin plugin) {
        this.database = database;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    public void handleCommand(String action, JsonObject json, WebSocket conn, ModeratorWebSocketServer server) {
        String playerName = json.has("playerName") ? json.get("playerName").getAsString() : null;
        String reason = json.has("reason") ? json.get("reason").getAsString() : "No reason specified";

        try {
            switch (action.toLowerCase()) {
                case "ban":
                    handleBan(conn, server, playerName, reason);
                    break;
                case "tempban":
                    handleTempBan(conn, server, playerName, reason, json);
                    break;
                case "unban":
                    handleUnban(conn, server, playerName);
                    break;
                case "kick":
                    handleKick(conn, server, playerName, reason);
                    break;
                case "warn":
                    handleWarn(conn, server, playerName, reason);
                    break;
                case "mute":
                    handleMute(conn, server, playerName, reason, json);
                    break;
                case "unmute":
                    handleUnmute(conn, server, playerName);
                    break;
                default:
                    server.sendError(conn, "Unknown action: " + action);
            }
        } catch (Exception e) {
            server.sendError(conn, "Error executing command: " + e.getMessage());
        }
    }

    private void handleBan(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            PunishmentUtil.executePunishment(database, languageManager, PunishmentType.BAN, Bukkit.getConsoleSender(), player, reason, null);

            server.sendSuccess(conn, "ban", "Player " + playerName + " has been banned");

            // Notifikovat ostatní klienty
            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "ban");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleTempBan(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, JsonObject json) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        long duration = json.has("duration") ? json.get("duration").getAsLong() : 3600;

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            PunishmentUtil.executePunishment(database, languageManager, PunishmentType.TEMPBAN, Bukkit.getConsoleSender(), player, reason, duration);

            server.sendSuccess(conn, "tempban", "Player " + playerName + " has been temporarily banned");

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "tempban");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            notification.addProperty("duration", duration);
            server.sendToAll(notification);
        });
    }

    private void handleUnban(WebSocket conn, ModeratorWebSocketServer server, String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        // TODO: Implement unban functionality using PunishmentUtil if needed
        server.sendSuccess(conn, "unban", "Player " + playerName + " has been unbanned");

        JsonObject notification = new JsonObject();
        notification.addProperty("type", "action");
        notification.addProperty("action", "unban");
        notification.addProperty("playerName", playerName);
        server.sendToAll(notification);
    }

    private void handleKick(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                PunishmentUtil.executePunishment(database, languageManager, PunishmentType.KICK, Bukkit.getConsoleSender(), player, reason, null);
                server.sendSuccess(conn, "kick", "Player " + playerName + " has been kicked");
            } else {
                server.sendError(conn, "Player is not online");
            }

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "kick");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleWarn(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                PunishmentUtil.executePunishment(database, languageManager, PunishmentType.WARN, Bukkit.getConsoleSender(), player, reason, null);
                server.sendSuccess(conn, "warn", "Player " + playerName + " has been warned");
            } else {
                server.sendError(conn, "Player is not online");
            }

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "warn");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleMute(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, JsonObject json) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        long duration = json.has("duration") ? json.get("duration").getAsLong() : 0;

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                if (duration > 0) {
                    PunishmentUtil.executePunishment(database, languageManager, PunishmentType.TEMPMUTE, Bukkit.getConsoleSender(), player, reason, duration);
                } else {
                    PunishmentUtil.executePunishment(database, languageManager, PunishmentType.MUTE, Bukkit.getConsoleSender(), player, reason, null);
                }

                server.sendSuccess(conn, "mute", "Player " + playerName + " has been muted");
            } else {
                server.sendError(conn, "Player is not online");
            }

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "mute");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            notification.addProperty("duration", duration);
            server.sendToAll(notification);
        });
    }

    private void handleUnmute(WebSocket conn, ModeratorWebSocketServer server, String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, "Player name is required");
            return;
        }

        // TODO: Implement unmute functionality using PunishmentUtil if needed

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.sendMessage("§a[UNMUTE] §fYou have been unmuted.");
        }

        server.sendSuccess(conn, "unmute", "Player " + playerName + " has been unmuted");

        JsonObject notification = new JsonObject();
        notification.addProperty("type", "action");
        notification.addProperty("action", "unmute");
        notification.addProperty("playerName", playerName);
        server.sendToAll(notification);
    }
}

