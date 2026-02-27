package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.managers.PermissionManager;
import com.etmisthefox.solacecore.models.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class PunishmentUtil {

    // Backward-compatible API used by inventories/GUI where sender and target are online Players
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, Player sender, Player target, String reason, Long durationSeconds) {
        // ...existing code...
        executePunishment(database, languageManager, punishmentType, sender, target != null ? target.getName() : null, target, reason, durationSeconds);
    }

    // New safe API for commands: supports console sender and offline targets (target may be null)
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, CommandSender sender, String targetName, Player target, String reason, Long durationSeconds) {
        PermissionManager perms = new PermissionManager();

        // Validate target presence for punishments that require the player to be online
        switch (punishmentType) {
            case KICK, MUTE, TEMPMUTE, WARN -> {
                if (target == null) {
                    sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
                    return;
                }
            }
            default -> {
                // BAN/TEMPBAN/IPBAN can proceed even if target is offline (target == null)
            }
        }

        // Resolve operator and targetName safely
        String operator = sender.getName();
        if (targetName == null && target != null) {
            targetName = target.getName();
        }
        if (targetName == null || targetName.isBlank()) {
            sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
            return;
        }

        // Target protection (handles null target internally)
        if (perms.hasProtection(target, punishmentType)) {
            switch (punishmentType) {
                case KICK -> sender.sendMessage(languageManager.getMessage("punishment.kick_protection", "player", target.getName()));
                case BAN, TEMPBAN, IPBAN -> sender.sendMessage(languageManager.getMessage("punishment.ban_protection", "player", target.getName()));
                case MUTE, TEMPMUTE -> sender.sendMessage(languageManager.getMessage("punishment.mute_protection", "player", target.getName()));
                case WARN -> sender.sendMessage(languageManager.getMessage("punishment.warn_protection", "player", target.getName()));
            }
            return;
        }

        // Prevent duplicate active punishments of same class (by name)
        if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.IPBAN || punishmentType == PunishmentType.MUTE || punishmentType == PunishmentType.TEMPMUTE) {
            try {
                List<Punishment> punishments = database.getActivePunishmentsByName(targetName);
                for (Punishment p : punishments) {
                    String type = p.getPunishmentType();
                    if (type.equals("ban") || type.equals("tempban") || type.equals("ipban")) {
                        sender.sendMessage(languageManager.getMessage("punishment.already_banned", "player", targetName));
                        return;
                    } else if (type.equals("mute") || type.equals("tempmute")) {
                        sender.sendMessage(languageManager.getMessage("punishment.already_muted", "player", targetName));
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        switch (punishmentType) {
            case BAN -> {
                if (target != null) {
                    target.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getMessage("player_messages.banned"), reason, operator, null));
                }
                Punishment punishment = new Punishment(0, targetName, reason, operator, "ban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.ban_success", "player", targetName, "reason", reason)));
            }
            case IPBAN -> {
                if (target != null) {
                    target.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getMessage("player_messages.ipbanned"), reason, operator, null));
                }
                Punishment punishment = new Punishment(0, targetName, reason, operator, "ipban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.ipban_success", "player", targetName, "reason", reason)));
            }
            case TEMPBAN -> {
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                if (target != null) {
                    target.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getMessage("player_messages.tempbanned"), reason, operator, formattedTime));
                }
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempban", start, end, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.tempban_success", "operator", operator, "player", targetName, "time", formattedTime, "reason", reason)));
            }
            case MUTE -> {
                // target validated above to be non-null for MUTE
                target.sendMessage(languageManager.getMessage("player_messages.muted", "reason", reason, "operator", operator));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "mute", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.mute_success", "player", targetName, "reason", reason)));
            }
            case TEMPMUTE -> {
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                // target validated above to be non-null for TEMPMUTE
                target.sendMessage(languageManager.getMessage("player_messages.tempmuted", "time", formattedTime, "reason", reason));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempmute", LocalDateTime.now(), null, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.tempmute_success", "operator", operator, "player", targetName, "time", formattedTime, "reason", reason)));
            }
            case KICK -> {
                // KICK requires online target (validated above)
                Punishment punishment = new Punishment(0, targetName, reason, operator, "kick", LocalDateTime.now(), null, null, false);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // target validated above to be non-null for KICK
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getMessage("player_messages.kicked"), reason, operator, null));
                Bukkit.broadcast(Component.text(languageManager.getMessage("broadcast.player_kicked", "player", targetName, "reason", reason)));
            }
            case WARN -> {
                // WARN requires online target (validated above)
                Punishment punishment = new Punishment(0, targetName, reason, operator, "warn", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // target validated above to be non-null for WARN
                target.sendMessage(languageManager.getMessage("player_messages.warned", "reason", reason, "operator", operator));
                Bukkit.broadcast(Component.text(languageManager.getMessage("broadcast.player_warned", "player", targetName, "reason", reason)));
            }
        }
    }

    private static String getPermissionNodeForPunishment(PunishmentType type) {
        switch (type) {
            case KICK:
                return PermissionManager.COMMAND_KICK;
            case BAN:
                return PermissionManager.COMMAND_BAN;
            case TEMPBAN:
                return PermissionManager.COMMAND_TEMPBAN;
            case IPBAN:
                return PermissionManager.COMMAND_IPBAN;
            case MUTE:
                return PermissionManager.COMMAND_MUTE;
            case TEMPMUTE:
                return PermissionManager.COMMAND_TEMPMUTE;
            case WARN:
                return PermissionManager.COMMAND_WARN;
            default:
                return null;
        }
    }
}
