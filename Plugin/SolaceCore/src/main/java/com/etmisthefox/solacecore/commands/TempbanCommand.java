package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
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

public final class TempbanCommand implements CommandExecutor {

    private final Database database;
    private final LanguageManager lang;

    public TempbanCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("solacecore.tempban")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("usage.tempban"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(lang.getMessage("errors.player_not_found"));
            return true;
        }

//        if (target.hasPermission("solacecore.banprotection")) {
//            sender.sendMessage(lang.getMessage("punishment.ban_protection", targetName));
//            return true;
//        }

        try {
            List<Punishment> punishments = database.getPunishmentsByName(targetName);
            for (Punishment punishment : punishments) {
                if (punishment.getPunishmentType().equals("ban") || punishment.getPunishmentType().equals("tempban")) {
                    sender.sendMessage(lang.getMessage("punishment.already_banned", "player", targetName));
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
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) reasonBuilder.append(' ');
        }
        String reason = reasonBuilder.isEmpty() ? lang.getMessage("no_reason") : reasonBuilder.toString();

        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPBAN, (Player) sender, target, reason, duration);
        return true;
    }
}
