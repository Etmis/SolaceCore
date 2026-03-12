package com.etmisthefox.solacecore.websocket;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
import com.etmisthefox.solacecore.models.Punishment;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.java_websocket.WebSocket;

import java.sql.SQLException;
import java.util.List;

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
        String reason = json.has("reason") ? json.get("reason").getAsString() : languageManager.getMessage("punishment.no_reason");
        String moderator = json.has("moderator") ? json.get("moderator").getAsString() : null;

        try {
            switch (action.toLowerCase()) {
                case "ban":
                    handleBan(conn, server, playerName, reason, moderator);
                    break;
                case "tempban":
                    handleTempBan(conn, server, playerName, reason, json, moderator);
                    break;
                case "unban":
                    handleUnban(conn, server, playerName);
                    break;
                case "kick":
                    handleKick(conn, server, playerName, reason, moderator);
                    break;
                case "warn":
                    handleWarn(conn, server, playerName, reason, moderator);
                    break;
                case "mute":
                    handleMute(conn, server, playerName, reason, json, moderator);
                    break;
                case "unmute":
                    handleUnmute(conn, server, playerName);
                    break;
                default:
                    server.sendError(conn, languageManager.getMessage("websocket.error.unknown_action", "action", action));
            }
        } catch (Exception e) {
            server.sendError(conn, languageManager.getMessage("websocket.error.executing_command", "error", e.getMessage()));
        }
    }

    private void handleBan(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, String moderator) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            PunishmentUtil.executePunishment(database, languageManager, PunishmentType.BAN, Bukkit.getConsoleSender(), player, reason, null, "web", moderator);

            server.sendSuccess(conn, "ban", languageManager.getMessage("websocket.success.ban", "player", playerName));

            // Notifikovat ostatní klienty
            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "ban");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleTempBan(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, JsonObject json, String moderator) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        long duration = json.has("duration") ? json.get("duration").getAsLong() : 3600;

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            PunishmentUtil.executePunishment(database, languageManager, PunishmentType.TEMPBAN, Bukkit.getConsoleSender(), player, reason, duration, "web", moderator);

            server.sendSuccess(conn, "tempban", languageManager.getMessage("websocket.success.tempban", "player", playerName));

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
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        try {
            List<Punishment> punishments = database.getActivePunishmentsByName(playerName);
            boolean unbanned = false;

            for (Punishment punishment : punishments) {
                String punishmentType = punishment.getPunishmentType();
                if ("ban".equalsIgnoreCase(punishmentType)
                        || "tempban".equalsIgnoreCase(punishmentType)
                        || "ipban".equalsIgnoreCase(punishmentType)
                        || "tempipban".equalsIgnoreCase(punishmentType)) {
                    database.unpunishPlayer(playerName, punishmentType.toLowerCase());
                    unbanned = true;
                }
            }

            if (!unbanned) {
                server.sendError(conn, languageManager.getMessage("websocket.error.player_not_banned", "player", playerName));
                return;
            }

            server.sendSuccess(conn, "unban", languageManager.getMessage("websocket.success.unban", "player", playerName));

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "unban");
            notification.addProperty("playerName", playerName);
            server.sendToAll(notification);
        } catch (SQLException e) {
            server.sendError(conn, languageManager.getMessage("websocket.error.unban_failed", "error", e.getMessage()));
        }
    }

    private void handleKick(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, String moderator) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                PunishmentUtil.executePunishment(database, languageManager, PunishmentType.KICK, Bukkit.getConsoleSender(), player, reason, null, "web", moderator);
                server.sendSuccess(conn, "kick", languageManager.getMessage("websocket.success.kick", "player", playerName));
            } else {
                server.sendError(conn, languageManager.getMessage("websocket.error.player_not_online", "player", playerName));
            }

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "kick");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleWarn(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, String moderator) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                PunishmentUtil.executePunishment(database, languageManager, PunishmentType.WARN, Bukkit.getConsoleSender(), player, reason, null, "web", moderator);
                server.sendSuccess(conn, "warn", languageManager.getMessage("websocket.success.warn", "player", playerName));
            } else {
                server.sendError(conn, languageManager.getMessage("websocket.error.player_not_online", "player", playerName));
            }

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "warn");
            notification.addProperty("playerName", playerName);
            notification.addProperty("reason", reason);
            server.sendToAll(notification);
        });
    }

    private void handleMute(WebSocket conn, ModeratorWebSocketServer server, String playerName, String reason, JsonObject json, String moderator) {
        if (playerName == null || playerName.isEmpty()) {
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        long duration = json.has("duration") ? json.get("duration").getAsLong() : 0;

        // Naplánovat na hlavní vlákno
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                if (duration > 0) {
                    PunishmentUtil.executePunishment(database, languageManager, PunishmentType.TEMPMUTE, Bukkit.getConsoleSender(), player, reason, duration, "web", moderator);
                } else {
                    PunishmentUtil.executePunishment(database, languageManager, PunishmentType.MUTE, Bukkit.getConsoleSender(), player, reason, null, "web", moderator);
                }

                server.sendSuccess(conn, "mute", languageManager.getMessage("websocket.success.mute", "player", playerName));
            } else {
                server.sendError(conn, languageManager.getMessage("websocket.error.player_not_online", "player", playerName));
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
            server.sendError(conn, languageManager.getMessage("websocket.error.player_name_required"));
            return;
        }

        try {
            List<Punishment> punishments = database.getActivePunishmentsByName(playerName);
            boolean unmuted = false;

            for (Punishment punishment : punishments) {
                String punishmentType = punishment.getPunishmentType();
                if ("mute".equalsIgnoreCase(punishmentType) || "tempmute".equalsIgnoreCase(punishmentType)) {
                    database.unpunishPlayer(playerName, punishmentType.toLowerCase());
                    unmuted = true;
                }
            }

            if (!unmuted) {
                server.sendError(conn, languageManager.getMessage("websocket.error.player_not_muted", "player", playerName));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    player.sendMessage(languageManager.getMessage("player_messages.unmuted", "operator", "web"));
                }
            });

            server.sendSuccess(conn, "unmute", languageManager.getMessage("websocket.success.unmute", "player", playerName));

            JsonObject notification = new JsonObject();
            notification.addProperty("type", "action");
            notification.addProperty("action", "unmute");
            notification.addProperty("playerName", playerName);
            server.sendToAll(notification);
        } catch (SQLException e) {
            server.sendError(conn, languageManager.getMessage("websocket.error.unmute_failed", "error", e.getMessage()));
        }
    }
}
