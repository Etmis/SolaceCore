package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

public final class UnbanCommand implements CommandExecutor {

    private final Database database;

    private final LanguageManager lang;

    public UnbanCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("solacecore.unban")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.unban"));
            return true;
        }

        String targetName = args[0];

        try {
            List<Punishment> punishments = database.getPunishmentsByName(targetName);

            for (Punishment punishment : punishments) {
                if (punishment.getIsActive()) {
                    if (punishment.getPunishmentType().equals("ban")) {
                        database.unpunishPlayer(targetName, "ban");
                        sender.sendMessage(lang.getMessage("punishment.unban_success", "player", targetName));
                    } else if (punishment.getPunishmentType().equals("tempban")) {
                        database.unpunishPlayer(targetName, "tempban");
                        sender.sendMessage(lang.getMessage("punishment.unban_success", "player", targetName));
                    } else {
                        sender.sendMessage(lang.getMessage("", "player", targetName));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }
}
