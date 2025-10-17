package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
            List<Punishment> punishments = database.getActivePunishmentsByName(targetName);

            if (punishments.isEmpty()) {
                sender.sendMessage(lang.getMessage("punishment.not_banned", "player", targetName));
                return true;
            }

            for (Punishment punishment : punishments) {
                PunishmentType punishmentType = PunishmentType.valueOf(punishment.getPunishmentType().toUpperCase());
                if (punishmentType == PunishmentType.BAN || punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.IPBAN) {
                    database.unpunishPlayer(targetName, punishmentType.toString().toLowerCase());
                    sender.sendMessage(lang.getMessage("punishment.unban_success", "player", targetName));
                }
                else {
                    sender.sendMessage(lang.getMessage("punishment.not_banned", "player", targetName));
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
