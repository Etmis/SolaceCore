package com.etmisthefox.solacecore.commands;

import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.inventories.MainMenu;
import com.etmisthefox.solacecore.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class MenuCommand implements CommandExecutor {

    private final Database database;
    private final LanguageManager lang;
    private final Plugin plugin;
    private final InventoryManager inventoryManager;

    public MenuCommand(Database database, LanguageManager lang, Plugin plugin, InventoryManager inventoryManager) {
        this.database = database;
        this.lang = lang;
        this.plugin = plugin;
        this.inventoryManager = inventoryManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getMessage("errors.only_players"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(lang.getMessage("usage.menu"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        // TODO: Support offline players

        if (!player.hasPermission("solacecore.menu")) {
            player.sendMessage(lang.getMessage("errors.no_permission"));
            return true;
        }

        MainMenu.getInventory(database, lang, plugin, inventoryManager, target).open(player);
        return true;
    }
}
