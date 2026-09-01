package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.managers.DiscordPermissionManager;
import com.etmisthefox.solacecore.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand implements CommandExecutor {

    private final SolaceCore plugin;
    private final LanguageManager lang;
    private final DiscordPermissionManager discordPermissionManager;

    public ReloadCommand(SolaceCore plugin, LanguageManager lang, DiscordPermissionManager discordPermissionManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.discordPermissionManager = discordPermissionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("solacecore.reload")) {
            sender.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(lang.getMessage("usage.reload"));
            return true;
        }

        String target = args[0].toLowerCase();
        switch (target) {
            case "language" -> {
                lang.reload();
                sender.sendMessage(lang.getMessage("reload.language_success"));
            }
            case "config" -> {
                plugin.reloadConfig();
                sender.sendMessage(lang.getMessage("reload.config_success"));
            }
            case "discord" -> {
                if (!plugin.getConfig().getBoolean("discord_bot.enabled", false)) {
                    sender.sendMessage(lang.getMessage("reload.discord_disabled"));
                    return true;
                }
                discordPermissionManager.reload();
                sender.sendMessage(lang.getMessage("reload.discord_success"));
            }
            default -> sender.sendMessage(lang.getMessage("usage.reload"));
        }
        return true;
    }
}
