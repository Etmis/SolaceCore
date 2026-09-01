package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.discord.DiscordManager;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.managers.PermissionManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class PunishmentUtil {

    private static void logToDiscord(String actionType, String operator, String targetName, String reason, String duration) {
        DiscordManager dm = DiscordManager.getInstance();
        if (dm != null) {
            dm.logActionToDiscord(actionType, operator, targetName, reason, duration);
        }
    }

    private static boolean isOnlineOnlyPunishment(PunishmentType punishmentType) {
        return switch (punishmentType) {
            case KICK, MUTE, TEMPMUTE, WARN -> true;
            default -> false;
        };
    }

    private static boolean isBanLikePunishment(PunishmentType punishmentType) {
        return switch (punishmentType) {
            case BAN, TEMPBAN, IPBAN, TEMPIPBAN -> true;
            default -> false;
        };
    }

    private static boolean isMuteLikePunishment(PunishmentType punishmentType) {
        return switch (punishmentType) {
            case MUTE, TEMPMUTE -> true;
            default -> false;
        };
    }

    private static void persistPunishment(Database database, Punishment punishment) {
        try {
            database.createPunishment(punishment);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void sendPlayerNotFound(LanguageManager languageManager, CommandSender sender) {
        if (sender != null) sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
    }

    // 1. Veřejná API metoda pro HRU (Konzole nebo Hráč)
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, CommandSender operator, String targetName, String reason, Long durationSeconds) {
        executeCore(database, languageManager, punishmentType, operator, operator.getName(), targetName, reason, durationSeconds);
    }

    // 2. Veřejná API metoda pro WEBSOCKET (Pouze jména ve Stringu)
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, String operatorName, String targetName, String reason, Long durationSeconds) {
        executeCore(database, languageManager, punishmentType, null, operatorName, targetName, reason, durationSeconds);
    }

    // 3. Skryté jádro logiky
    private static void executeCore(Database database, LanguageManager languageManager, PunishmentType punishmentType, CommandSender operator, String operatorName, String targetName, String reason, Long durationSeconds) {
        PermissionManager perms = new PermissionManager();
        Player targetPlayer = Bukkit.getPlayerExact(targetName);

        if (isOnlineOnlyPunishment(punishmentType) && targetPlayer == null) {
            sendPlayerNotFound(languageManager, operator);
            return;
        }

        try {
            if (!database.playerExistsByName(targetName)) {
                sendPlayerNotFound(languageManager, operator);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendPlayerNotFound(languageManager, operator);
            return;
        }

        if (targetPlayer != null && perms.hasProtection(targetPlayer, punishmentType)) {
            if (operator != null) {
                switch (punishmentType) {
                    case KICK -> operator.sendMessage(languageManager.getMessage("protection.kick_protection", "player", targetName));
                    case BAN, TEMPBAN, IPBAN, TEMPIPBAN -> operator.sendMessage(languageManager.getMessage("protection.ban_protection", "player", targetName));
                    case MUTE, TEMPMUTE -> operator.sendMessage(languageManager.getMessage("protection.mute_protection", "player", targetName));
                    case WARN -> operator.sendMessage(languageManager.getMessage("protection.warn_protection", "player", targetName));
                }
            }
            return;
        }

        if (isBanLikePunishment(punishmentType) || isMuteLikePunishment(punishmentType)) {
            try {
                List<Punishment> punishments = database.getActivePunishmentsByName(targetName);
                for (Punishment p : punishments) {
                    PunishmentType type = PunishmentType.valueOf(p.getPunishmentType().toUpperCase());
                    if (isBanLikePunishment(type) && isBanLikePunishment(punishmentType)) {
                        if (operator != null) operator.sendMessage(languageManager.getMessage("punishment.already_banned", "player", targetName));
                        return;
                    } else if (isMuteLikePunishment(type) && isMuteLikePunishment(punishmentType)) {
                        if (operator != null) operator.sendMessage(languageManager.getMessage("punishment.already_muted", "player", targetName));
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        switch (punishmentType) {
            case BAN -> {
                if (targetPlayer != null) targetPlayer.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getRawMessage("player_messages.banned"), reason, operatorName, null));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "ban", LocalDateTime.now(), null, null, true);
                persistPunishment(database, punishment);
                logToDiscord("BAN", operatorName, targetName, reason, null);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.ban_success", "player", targetName, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_banned", "player", targetName, "reason", reason, "operator", operatorName));
            }
            case IPBAN -> {
                if (targetPlayer != null) targetPlayer.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getRawMessage("player_messages.ipbanned"), reason, operatorName, null));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "ipban", LocalDateTime.now(), null, null, true);
                persistPunishment(database, punishment);
                logToDiscord("IPBAN", operatorName, targetName, reason, null);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.ipban_success", "player", targetName, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_ipbanned", "player", targetName, "reason", reason, "operator", operatorName));
            }
            case TEMPIPBAN -> {
                if (operator != null && !operator.hasPermission("solacecore.tempipban")) {
                    operator.sendMessage(languageManager.getMessage("errors.no_permission"));
                    return;
                }
                if (durationSeconds == null || durationSeconds <= 0) {
                    if (operator != null) operator.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                if (targetPlayer != null) targetPlayer.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getRawMessage("player_messages.tempipban"), reason, operatorName, formattedTime));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "tempipban", start, end, durationSeconds, true);
                persistPunishment(database, punishment);
                logToDiscord("TEMPIPBAN", operatorName, targetName, reason, formattedTime);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.tempipban_success", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_tempipbanned", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
            }
            case TEMPBAN -> {
                if (durationSeconds == null || durationSeconds <= 0) {
                    if (operator != null) operator.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                if (targetPlayer != null) targetPlayer.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getRawMessage("player_messages.tempbanned"), reason, operatorName, formattedTime));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "tempban", start, end, durationSeconds, true);
                persistPunishment(database, punishment);
                logToDiscord("TEMPBAN", operatorName, targetName, reason, formattedTime);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.tempban_success", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_tempbanned", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
            }
            case MUTE -> {
                if (targetPlayer != null) targetPlayer.sendMessage(languageManager.getMessage("player_messages.muted", "reason", reason, "operator", operatorName));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "mute", LocalDateTime.now(), null, null, true);
                persistPunishment(database, punishment);
                logToDiscord("MUTE", operatorName, targetName, reason, null);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.mute_success", "player", targetName, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_muted", "player", targetName, "reason", reason, "operator", operatorName));
            }
            case TEMPMUTE -> {
                if (durationSeconds == null || durationSeconds <= 0) {
                    if (operator != null) operator.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                if (targetPlayer != null) targetPlayer.sendMessage(languageManager.getMessage("player_messages.tempmuted", "time", formattedTime, "reason", reason, "operator", operatorName));
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "tempmute", LocalDateTime.now(), null, durationSeconds, true);
                persistPunishment(database, punishment);
                logToDiscord("TEMPMUTE", operatorName, targetName, reason, formattedTime);
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.tempmute_success", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.tempmute_success", "player", targetName, "time", formattedTime, "reason", reason, "operator", operatorName));
            }
            case KICK -> {
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "kick", LocalDateTime.now(), null, null, false);
                persistPunishment(database, punishment);
                logToDiscord("KICK", operatorName, targetName, reason, null);
                if (targetPlayer != null) targetPlayer.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getRawMessage("player_messages.kicked"), reason, operatorName, null));
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.kick_success", "player", targetName, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_kicked", "player", targetName, "reason", reason, "operator", operatorName));
            }
            case WARN -> {
                Punishment punishment = new Punishment(0, targetName, reason, operatorName, "warn", LocalDateTime.now(), null, null, true);
                persistPunishment(database, punishment);
                logToDiscord("WARN", operatorName, targetName, reason, null);
                if (targetPlayer != null) targetPlayer.sendMessage(languageManager.getMessage("player_messages.warned", "reason", reason, "operator", operatorName));
                if (operator != null) operator.sendMessage(languageManager.getMessage("moderator_messages.warn_success", "player", targetName, "reason", reason, "operator", operatorName));
                Bukkit.broadcast(languageManager.getMessage("broadcast.player_warned", "player", targetName, "reason", reason, "operator", operatorName));
            }
        }
    }
}