package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.discord.DiscordManager;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
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
        executePunishment(database, lang, punishmentType, sender, target, reason, durationSeconds, "ingame");
    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, CommandSender sender, Player target, String reason, Long durationSeconds, String source) {
        if (target == null) {
            sender.sendMessage(lang.getMessage("errors.player_not_found"));
            return;
        }

        String operator = sender.getName();
        String targetName = target.getName();

        if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.IPBAN || punishmentType == PunishmentType.TEMPIPBAN || punishmentType == PunishmentType.MUTE || punishmentType == PunishmentType.TEMPMUTE) {
            try {
                List<Punishment> punishments = database.getActivePunishmentsByName(targetName);
                for (Punishment p : punishments) {
                    String type = p.getPunishmentType();
                    if (type.equals("ban") || type.equals("tempban") || type.equals("ipban") || type.equals("tempipban")) {
                        sender.sendMessage(lang.getMessage("punishment.already_banned", "player", targetName));
                        return;
                    }
                    else if (type.equals("mute") || type.equals("tempmute")) {
                        sender.sendMessage(lang.getMessage("punishment.already_muted", "player", targetName));
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        switch (punishmentType) {
            case BAN -> {
                if (!sender.hasPermission("solacecore.ban")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.banned", "reason", reason, "operator", operator), reason, operator, null));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "ban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("BAN", operator, targetName, reason, source);
                    logToDiscord("BAN", operator, targetName, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.ban_success", "player", targetName, "reason", reason)));
            }
            case IPBAN -> {
                if (!sender.hasPermission("solacecore.ipban")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.ipbanned", "reason", reason, "operator", operator), reason, operator, null));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "ipban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("IPBAN", operator, targetName, reason, source);
                    logToDiscord("IPBAN", operator, targetName, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.ipban_success", "player", targetName, "reason", reason)));
            }
            case TEMPIPBAN -> {
                if (!sender.hasPermission("solacecore.tempipban")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(lang.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.tempipbanned", "time", formattedTime, "reason", reason, "operator", operator), reason, operator, formattedTime));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempipban", start, end, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("TEMPIPBAN", operator, targetName, reason + " (Duration: " + formattedTime + ")", source);
                    logToDiscord("TEMPIPBAN", operator, targetName, reason, formattedTime);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.tempipban_success", "operator", operator, "player", targetName, "time", formattedTime, "reason", reason)));
            }
            case TEMPBAN -> {
                if (!sender.hasPermission("solacecore.tempban")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(lang.getMessage("errors.invalid_time"));
                    return;
                }
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end = start.plusSeconds(durationSeconds);
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.tempbanned", "time", formattedTime, "reason", reason, "operator", operator), reason, operator, formattedTime));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempban", start, end, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("TEMPBAN", operator, targetName, reason + " (Duration: " + formattedTime + ")", source);
                    logToDiscord("TEMPBAN", operator, targetName, reason, formattedTime);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.tempban_success", "operator", operator, "player", targetName, "time", formattedTime, "reason", reason)));
            }
            case MUTE -> {
                if (!sender.hasPermission("solacecore.mute")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                target.sendMessage(lang.getMessage("player_messages.muted", "reason", reason, "operator", operator));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "mute", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("MUTE", operator, targetName, reason, source);
                    logToDiscord("MUTE", operator, targetName, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.mute_success", "player", targetName, "reason", reason)));
            }
            case TEMPMUTE -> {
                if (!sender.hasPermission("solacecore.tempmute")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                if (durationSeconds == null || durationSeconds <= 0) {
                    sender.sendMessage(lang.getMessage("errors.invalid_time"));
                    return;
                }
                String formattedTime = TimeUtil.formatDuration(durationSeconds);
                target.sendMessage(lang.getMessage("player_messages.tempmuted", "time", formattedTime, "reason", reason, "operator", operator));
                Punishment punishment = new Punishment(0, targetName, reason, operator, "tempmute", LocalDateTime.now(), null, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("TEMPMUTE", operator, targetName, reason + " (Duration: " + formattedTime + ")", source);
                    logToDiscord("TEMPMUTE", operator, targetName, reason, formattedTime);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcast(Component.text(lang.getMessage("punishment.tempmute_success", "operator", sender.getName(), "player", targetName, "time", formattedTime, "reason", reason)));
            }
            case KICK -> {
                if (!sender.hasPermission("solacecore.kick")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                Punishment punishment = new Punishment(0, targetName, reason, operator, "kick", LocalDateTime.now(), null, null, false);
                try {
                    database.createPunishment(punishment);
                    database.logAction("KICK", operator, targetName, reason, source);
                    logToDiscord("KICK", operator, targetName, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                target.kick(DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.kicked"), reason, operator, null));
                Bukkit.broadcast(Component.text(lang.getMessage("broadcast.player_kicked", "player", targetName, "reason", reason)));
            }
            case WARN -> {
                Punishment punishment = new Punishment(0, targetName, reason, operator, "warn", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                    database.logAction("WARN", operator, targetName, reason, source);
                    logToDiscord("WARN", operator, targetName, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                target.sendMessage(lang.getMessage("player_messages.warned", "reason", reason, "operator", operator));
                Bukkit.broadcast(Component.text(lang.getMessage("broadcast.player_warned", "player", targetName, "reason", reason)));
            }
        }
    }
}
