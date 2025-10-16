package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class PunishmentUtil {

    // Původní API zachováno – volá novou metodu bez duration
//    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, Player sender, Player target, String reason) {
//        executePunishment(database, lang, punishmentType, sender, target, reason, null);
//    }

    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, Player sender, Player target, String reason, Long durationSeconds) {
        if (target == null) {
            sender.sendMessage(lang.getMessage("errors.player_not_found"));
            return;
        }

        String targetName = target.getName();

        // Společná kontrola existujícího ban/tempban pro ban typy
        if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN) {
            try {
                List<Punishment> punishments = database.getPunishmentsByName(targetName);
                for (Punishment p : punishments) {
                    String type = p.getPunishmentType();
                    if ("ban".equalsIgnoreCase(type) || "tempban".equalsIgnoreCase(type)) {
                        sender.sendMessage(lang.getMessage("punishment.already_banned", "player", targetName));
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

                target.kickPlayer(lang.getMessage("player_messages.banned", "reason", reason, "operator", sender.getName()));
                Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "ban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcastMessage(lang.getMessage("punishment.ban_success", "player", targetName, "reason", reason));
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
                String formatted = TimeUtil.formatDuration(durationSeconds);
                target.kickPlayer(lang.getMessage("player_messages.tempbanned", "time", formatted, "reason", reason, "operator", sender.getName()));
                Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "tempban", start, end, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcastMessage(lang.getMessage("punishment.tempban_success", "operator", sender.getName(), "player", targetName, "time", formatted, "reason", reason));
            }
            case IPBAN -> {
                // Použijeme stejné oprávnění jako pro BAN
                if (!sender.hasPermission("solacecore.ipban")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                String operator = sender.getName();
                // Informace hráči (kick)
                target.kickPlayer(lang.getMessage("player_messages.ipbanned") != null
                        ? lang.getMessage("player_messages.ipbanned", "reason", reason, "operator", operator)
                        : lang.getMessage("player_messages.banned", "reason", reason, "operator", operator));
                // Zápis trestu
                Punishment punishment = new Punishment(0, targetName, reason, operator, "ipban", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // Broadcast – pokud není definovaná ipban zpráva, použijeme obecný ban_success
                String broadcast = lang.getMessage("punishment.ipban_success") != null
                        ? lang.getMessage("punishment.ipban_success", "player", targetName, "reason", reason)
                        : lang.getMessage("punishment.ban_success", "player", targetName, "reason", reason);
                Bukkit.broadcastMessage(broadcast);
            }
            case MUTE -> {
                if (!sender.hasPermission("solacecore.mute")) {
                    sender.sendMessage(lang.getMessage("errors.no_permission"));
                    return;
                }
                // Kontrola už existujícího mute/tempmute
                try {
                    List<Punishment> punishments = database.getPunishmentsByName(targetName);
                    for (Punishment p : punishments) {
                        String type = p.getPunishmentType();
                        if ("mute".equalsIgnoreCase(type) || "tempmute".equalsIgnoreCase(type)) {
                            sender.sendMessage(lang.getMessage("punishment.already_muted", "player", targetName));
                            return;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                target.sendMessage(lang.getMessage("player_messages.muted", "reason", reason));
                Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "mute", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcastMessage(lang.getMessage("punishment.mute_success", "player", targetName, "reason", reason));
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
                // Kontrola už existujícího mute/tempmute
                try {
                    List<Punishment> punishments = database.getPunishmentsByName(targetName);
                    for (Punishment p : punishments) {
                        String type = p.getPunishmentType();
                        if ("mute".equalsIgnoreCase(type) || "tempmute".equalsIgnoreCase(type)) {
                            sender.sendMessage(lang.getMessage("punishment.already_muted", "player", targetName));
                            return;
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                String formatted = TimeUtil.formatDuration(durationSeconds);
                target.sendMessage(lang.getMessage("player_messages.tempmuted", "time", formatted, "reason", reason));
                Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "tempmute", LocalDateTime.now(), null, durationSeconds, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Bukkit.broadcastMessage(lang.getMessage("punishment.tempmute_success", "operator", sender.getName(), "player", targetName, "time", formatted, "reason", reason));
            }
            case KICK -> {
                target.kickPlayer(lang.getMessage("player_messages.kicked", "reason", reason));
                Bukkit.broadcastMessage(lang.getMessage("punishment.kick_success", "player", targetName, "reason", reason));
            }
            case WARN -> {
                // WARN nemění stav v DB nijak kriticky, ale uložíme pro historii
                Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "warn", LocalDateTime.now(), null, null, true);
                try {
                    database.createPunishment(punishment);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // Zpráva hráči
                String playerMsg = lang.getMessage("player_messages.warned");
                if (playerMsg == null) {
                    target.sendMessage("Byl jsi varován! Důvod: " + reason);
                } else {
                    target.sendMessage(lang.getMessage("player_messages.warned", "reason", reason, "operator", sender.getName()));
                }
                // Broadcast
                String broadcast = lang.getMessage("punishment.warn_success");
                if (broadcast == null) {
                    Bukkit.broadcastMessage("Hráč " + targetName + " byl varován. Důvod: " + reason);
                } else {
                    Bukkit.broadcastMessage(lang.getMessage("punishment.warn_success", "player", targetName, "reason", reason));
                }
            }
        }
    }
}
