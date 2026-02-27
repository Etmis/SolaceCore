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

public final class MuteCommand implements CommandExecutor {

    private final Database database;
    private final LanguageManager lang;

    public MuteCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.mute"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (byte i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) reasonBuilder.append(" ");
        }
        String reason = reasonBuilder.isEmpty() ? lang.getMessage("punishment.no_reason") : reasonBuilder.toString();

        Player target = Bukkit.getPlayer(args[0]);
        PunishmentUtil.executePunishment(database, lang, PunishmentType.MUTE, sender, args[0], target, reason, null);
        return true;
    }
}
