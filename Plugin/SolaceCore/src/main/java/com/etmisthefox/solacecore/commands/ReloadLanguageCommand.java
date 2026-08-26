package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.managers.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ReloadLanguageCommand implements CommandExecutor {

    private final SolaceCore plugin;
    private final LanguageManager lang;

    public ReloadLanguageCommand(SolaceCore plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("solacecore.reloadlang")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        plugin.reloadConfig();
        lang.reload();
        sender.sendMessage(ChatColor.GREEN + "Language files reloaded successfully.");
        return true;
    }
}
