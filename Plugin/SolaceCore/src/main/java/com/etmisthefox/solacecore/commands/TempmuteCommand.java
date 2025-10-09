package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static com.etmisthefox.solacecore.utils.TimeUtil.formatDuration;
import static com.etmisthefox.solacecore.utils.TimeUtil.parseDuration;

public final class TempmuteCommand implements CommandExecutor {

    private final Database database;

    private final LanguageManager lang;

    public TempmuteCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("solacecore.tempmute")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("usage.tempmute"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(lang.getMessage("errors.player_not_found"));
            return true;
        }

//        if (target.hasPermission("solacecore.muteprotection")) {
//            sender.sendMessage(lang.getMessage("punishment.mute_protection", targetName));
//            return true;
//        }

        try {
            List<Punishment> punishments = database.getPunishmentsByName(targetName);

            for (Punishment punishment : punishments) {
                if (punishment.getPunishmentType().equals("mute") || punishment.getPunishmentType().equals("tempmute")) {
                    sender.sendMessage(lang.getMessage("punishment.already_muted", "player", targetName));
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        long duration = parseDuration(args[1]);

        if (duration <= 0) {
            sender.sendMessage(lang.getMessage("errors.invalid_time"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();

        for (byte i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) {
                reasonBuilder.append(" ");
            }
        }

        String reason = reasonBuilder.isEmpty() ? lang.getMessage("no_reason") : reasonBuilder.toString();

        target.sendMessage(lang.getMessage("player_messages.tempmuted", "time", formatDuration(duration), "reason", reason));
        Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "tempmute", LocalDateTime.now(), null, duration, true);
        try {
            database.createPunishment(punishment);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Use formatted duration for broadcast
        Bukkit.broadcastMessage(lang.getMessage("punishment.tempmute_success", "operator", sender.getName(), "player", targetName, "time", formatDuration(duration), "reason", reason));

        return true;
    }
}
