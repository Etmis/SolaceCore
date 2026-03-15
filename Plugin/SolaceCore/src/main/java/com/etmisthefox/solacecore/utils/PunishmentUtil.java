package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.discord.DiscordManager;
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

    private static void logToDiscord(String actionType, String operator, String targetName, String reason, String duration) {
        DiscordManager dm = DiscordManager.getInstance();
        if (dm != null) {
            dm.logActionToDiscord(actionType, operator, targetName, reason, duration);
        }
    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, CommandSender sender, Player target, String reason, Long durationSeconds) {
        executePunishment(database, lang, punishmentType, sender, target, null, reason, durationSeconds, "ingame");
    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, CommandSender sender, Player target, String reason, Long durationSeconds, String source) {
        executePunishment(database, lang, punishmentType, sender, target, null, reason, durationSeconds, source, null);
    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, CommandSender sender, Player target, String targetNameOverride, String reason, Long durationSeconds) {
        executePunishment(database, lang, punishmentType, sender, target, targetNameOverride, reason, durationSeconds, "ingame", null);
    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, CommandSender sender, Player target, String targetNameOverride, String reason, Long durationSeconds, String source) {
        executePunishment(database, lang, punishmentType, sender, target, targetNameOverride, reason, durationSeconds, source, null);
    }

    // New safe API for commands: supports console sender and offline targets (target may be null)
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, CommandSender sender, Player target, String reason, Long durationSeconds, String source, String operatorOverride) {
        executePunishment(database, languageManager, punishmentType, sender, target, null, reason, durationSeconds, source, operatorOverride);
    }

    // New safe API for commands: supports console sender and offline targets by explicit target name
    public static void executePunishment(Database database, LanguageManager languageManager, PunishmentType punishmentType, CommandSender sender, Player target, String targetNameOverride, String reason, Long durationSeconds, String source, String operatorOverride) {
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
        String targetName = targetNameOverride;
        // Resolve operator and targetName safely
        String operator = sender.getName();
        if (operatorOverride != null && !operatorOverride.isBlank()) {
            operator = operatorOverride;
        }
        if (target != null) {
            targetName = target.getName();
        }
        if (targetName == null || targetName.isBlank()) {
            sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
            return;
        }

        // FK safeguard: reject punishments for players that were never stored in players table.
        try {
            if (!database.playerExistsByName(targetName)) {
                sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(languageManager.getMessage("errors.player_not_found"));
            return;
        }

        // Target protection (handles null target internally)
        if (perms.hasProtection(target, punishmentType)) {
            switch (punishmentType) {
                case KICK -> sender.sendMessage(languageManager.getMessage("punishment.kick_protection", "player", targetName));
                case BAN, TEMPBAN, IPBAN, TEMPIPBAN -> sender.sendMessage(languageManager.getMessage("punishment.ban_protection", "player", targetName));
                case MUTE, TEMPMUTE -> sender.sendMessage(languageManager.getMessage("punishment.mute_protection", "player", targetName));
                case WARN -> sender.sendMessage(languageManager.getMessage("punishment.warn_protection", "player", targetName));
            }
            return;
        }

        // Prevent duplicate active punishments of same class (by name)
        if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.IPBAN || punishmentType == PunishmentType.TEMPIPBAN || punishmentType == PunishmentType.MUTE || punishmentType == PunishmentType.TEMPMUTE) {
            try {
                List<Punishment> punishments = database.getActivePunishmentsByName(targetName);
                for (Punishment p : punishments) {
                    PunishmentType type = PunishmentType.valueOf(p.getPunishmentType().toUpperCase());
                    if ((type.equals(PunishmentType.BAN) || type.equals(PunishmentType.TEMPBAN) || type.equals(PunishmentType.IPBAN) || type.equals(PunishmentType.TEMPIPBAN)) && (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.IPBAN || punishmentType == PunishmentType.TEMPIPBAN)) {
                        sender.sendMessage(languageManager.getMessage("punishment.already_banned", "player", targetName));
                        return;
                    } else if ((type.equals(PunishmentType.MUTE) || type.equals(PunishmentType.TEMPMUTE)) && (punishmentType == PunishmentType.MUTE || punishmentType == PunishmentType.TEMPMUTE)) {
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
                logToDiscord("BAN", operator, targetName, reason, null);
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
                logToDiscord("IPBAN", operator, targetName, reason, null);
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.ipban_success", "player", targetName, "reason", reason)));
            }
            case TEMPIPBAN -> {
                if (!sender.hasPermission("solacecore.tempipban")) {
                    sender.sendMessage(languageManager.getMessage("errors.no_permission"));
                    return;
                }
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                if (target != null) {
                    target.kick(DisconnectScreenUtil.formatDisconnectScreen(true, languageManager.getMessage("player_messages.tempipban"), reason, operator, formattedTime));
                }
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempipban", start, end, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                logToDiscord("TEMPIPBAN", operator, targetName, reason, formattedTime);
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.tempipban_success", "operator", operator, "player", targetName, "time", formattedTime, "reason", reason)));
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
                logToDiscord("TEMPBAN", operator, targetName, reason, formattedTime);
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
                logToDiscord("MUTE", operator, targetName, reason, null);
                Bukkit.broadcast(Component.text(languageManager.getMessage("punishment.mute_success", "player", targetName, "reason", reason)));
            }
            case TEMPMUTE -> {
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(languageManager.getMessage("errors.invalid_time"));
                    return;
                }
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                target.sendMessage(languageManager.getMessage("player_messages.tempmuted", "time", formattedTime, "reason", reason, "operator", operator));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempmute", LocalDateTime.now(), null, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                logToDiscord("TEMPMUTE", operator, targetName, reason, formattedTime);
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
                logToDiscord("KICK", operator, targetName, reason, null);
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
                logToDiscord("WARN", operator, targetName, reason, null);
                // target validated above to be non-null for WARN
                target.sendMessage(languageManager.getMessage("player_messages.warned", "reason", reason, "operator", operator));
                Bukkit.broadcast(Component.text(languageManager.getMessage("broadcast.player_warned", "player", targetName, "reason", reason)));
            }
        }
    }
}
