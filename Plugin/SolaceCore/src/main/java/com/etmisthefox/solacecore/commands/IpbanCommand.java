package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class IpbanCommand implements CommandExecutor {

    private final Database database;
    private final LanguageManager lang;

    public IpbanCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.ipban"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (byte i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) reasonBuilder.append(" ");
        }
        String reason = reasonBuilder.isEmpty() ? lang.getMessage("punishment.no_reason") : reasonBuilder.toString();

        PunishmentUtil.executePunishment(database, lang, PunishmentType.IPBAN, (Player) sender, Bukkit.getPlayer(args[0]), reason, null);
        return true;
    }
}
