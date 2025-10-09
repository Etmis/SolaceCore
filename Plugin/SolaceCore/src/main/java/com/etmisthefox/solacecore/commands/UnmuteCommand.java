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
import java.util.List;

public final class UnmuteCommand implements CommandExecutor {

    private final Database database;

    private final LanguageManager lang;

    public UnmuteCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("solacecore.unmute")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.unmute"));
            return true;
        }

        String targetName = args[0];

        try {

            List<Punishment> punishments = database.getPunishmentsByName(targetName);

            for (Punishment punishment : punishments) {
                if (punishment.getIsActive()) {
                    if (punishment.getPunishmentType().equals("mute")) {
                        database.unpunishPlayer(targetName, "mute");
                    } else if (punishment.getPunishmentType().equals("tempmute")) {
                        database.unpunishPlayer(targetName, "tempmute");
                    } else {
                        sender.sendMessage(lang.getMessage("punishment.not_muted", "player", targetName));
                        continue;
                    }
                    sender.sendMessage(lang.getMessage("punishment.unmute_success", "player", targetName));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }
}
