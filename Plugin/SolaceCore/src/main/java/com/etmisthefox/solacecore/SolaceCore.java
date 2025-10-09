package com.etmisthefox.solacecore;

import com.etmisthefox.solacecore.commands.*;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.solacecore.listeners.ChatListener;
import com.etmisthefox.solacecore.listeners.ConnectionListener;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.ChatInputUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class SolaceCore extends JavaPlugin {

    private LanguageManager lang;
    private Database database;
    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        lang = new LanguageManager(this, getConfig().getString("language", "en"));

        database = new Database(this);
        try {
            database.initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Database error...");
        }

        getCommand("kick").setExecutor(new KickCommand(database, lang));
        getCommand("ban").setExecutor(new BanCommand(database, lang));
        getCommand("tempban").setExecutor(new TempbanCommand(database, lang));
        getCommand("unban").setExecutor(new UnbanCommand(database, lang));
        getCommand("mute").setExecutor(new MuteCommand(database, lang));
        getCommand("tempmute").setExecutor(new TempmuteCommand(database, lang));
        getCommand("unmute").setExecutor(new UnmuteCommand(database, lang));
        getCommand("menu").setExecutor(new MenuCommand(database, lang, inventoryManager));

        getServer().getPluginManager().registerEvents(new ConnectionListener(database, lang), this);
        getServer().getPluginManager().registerEvents(new ChatListener(database, lang), this);
    }

    @Override
    public void onDisable() {
        // Zrušení případných čekajících chat inputů
        ChatInputUtil.cancelAll();
        if (database != null) {
            database.closeConnection();
        }
    }
}
