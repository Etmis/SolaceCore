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
        if (args.length < 2) {
            sender.sendMessage(lang.getMessage("usage.tempmute"));
            return true;
        }

        long duration = parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(lang.getMessage("errors.invalid_time"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (byte i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]);
            if (i < args.length - 1) reasonBuilder.append(" ");
        }
        String reason = reasonBuilder.isEmpty() ? lang.getMessage("punishment.no_reason") : reasonBuilder.toString();

        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPMUTE, (Player) sender, Bukkit.getPlayer(args[0]), reason, duration);
        return true;
    }
}
