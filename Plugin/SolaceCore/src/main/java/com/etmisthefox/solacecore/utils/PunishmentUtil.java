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
    public static void executePunishment(Database database, LanguageManager lang, PunishmentType punishmentType, Player sender, Player target, String reason) {
        executePunishment(database, lang, punishmentType, sender, target, reason, null);
    }

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
                // Permanent ban
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
                // TODO: implement IP ban
            }
            case MUTE -> {
                // TODO: implement mute
            }
            case TEMPMUTE -> {
                // TODO: implement temp mute
            }
            case KICK -> {
                target.kickPlayer(lang.getMessage("player_messages.kicked", "reason", reason));
                Bukkit.broadcastMessage(lang.getMessage("punishment.kick_success", "player", targetName, "reason", reason));
            }
            case WARN -> {
                // TODO: implement warn
            }
        }
    }
}
