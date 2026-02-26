package com.etmisthefox.solacecore;

import com.etmisthefox.solacecore.commands.*;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.solacecore.listeners.ChatListener;
import com.etmisthefox.solacecore.listeners.ConnectionListener;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.ChatInputUtil;
import com.etmisthefox.solacecore.utils.DisconnectScreenUtil;
import com.etmisthefox.solacecore.discord.DiscordManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class SolaceCore extends JavaPlugin {

    private Database database;
    private LanguageManager lang;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        InventoryManager inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        DisconnectScreenUtil.setFc(getConfig());

        lang = new LanguageManager(this, getConfig().getString("language", "en"));

        database = new Database(this);
        try {
            database.initializeDatabase();
        } catch (SQLException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Database error", e);
            getLogger().severe("Database error...");
            getServer().shutdown();
        }

        if (getConfig().getBoolean("discord_bot.enabled", false)) {
            try {
                discordManager = new DiscordManager(this, database, lang);
                discordManager.initialize();
            } catch (InterruptedException e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize Discord bot", e);
            }
        }

        registerCommand("kick", new KickCommand(database, lang));
        registerCommand("ban", new BanCommand(database, lang));
        registerCommand("ipban", new IpbanCommand(database, lang));
        registerCommand("tempban", new TempbanCommand(database, lang));
        registerCommand("tempipban", new TempipbanCommand(database, lang));
        registerCommand("unban", new UnbanCommand(database, lang));
        registerCommand("mute", new MuteCommand(database, lang));
        registerCommand("warn", new WarnCommand(database, lang));
        registerCommand("tempmute", new TempmuteCommand(database, lang));
        registerCommand("unmute", new UnmuteCommand(database, lang));
        registerCommand("menu", new MenuCommand(database, lang, this, inventoryManager));
        registerCommand("warns", new WarnsCommand(database, lang));

        getServer().getPluginManager().registerEvents(new ConnectionListener(database, lang), this);
        getServer().getPluginManager().registerEvents(new ChatListener(database, lang), this);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' not found in plugin.yml!");
            return;
        }
        command.setExecutor(executor);
    }


    @Override
    public void onDisable() {
        ChatInputUtil.cancelAll();
        if (database != null) {
            database.closeConnection();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
