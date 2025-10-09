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

public final class KickCommand implements CommandExecutor {

    private final Database database;

    private final LanguageManager lang;

    public KickCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender.hasPermission("solacecore.kick"))) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.kick"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(lang.getMessage("errors.player_not_found"));
            return true;
        }

//        if (target.hasPermission("solacecore.kickprotection")) {
//            sender.sendMessage(lang.getMessage("punishment.kick_protection", "player", targetName));
//            return true;
//        }

        StringBuilder reasonBuilder = new StringBuilder();

        for (byte i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) {
                reasonBuilder.append(" ");
            }
        }

        String reason = reasonBuilder.isEmpty() ? lang.getMessage("no_reason") : reasonBuilder.toString();

        Punishment punishment = new Punishment(0, target.getName(), reason, sender.getName(), "kick", LocalDateTime.now(), LocalDateTime.now(), null, false);

        try {
            database.createPunishment(punishment);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        target.kickPlayer(lang.getMessage("player_messages.kicked", "reason", reason));
        sender.sendMessage(lang.getMessage("punishment.kick_success", "player", target.getName(), "reason", reason));
        return true;
    }
}
