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

public final class MuteCommand implements CommandExecutor {

    private final Database database;
    private final LanguageManager lang;

    public MuteCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("solacecore.mute")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.mute"));
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

        StringBuilder reasonBuilder = new StringBuilder();

        for (byte i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) {
                reasonBuilder.append(" ");
            }
        }

        String reason = reasonBuilder.isEmpty() ? lang.getMessage("no_reason") : reasonBuilder.toString();

        target.sendMessage(lang.getMessage("player_messages.muted", "reason", reason));

        Punishment punishment = new Punishment(0, targetName, reason, sender.getName(), "mute", LocalDateTime.now(), null, null, true);
        try {
            database.createPunishment(punishment);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Bukkit.broadcastMessage(lang.getMessage("punishment.mute_success", "player", targetName, "reason", reason));
        return true;
    }

}
