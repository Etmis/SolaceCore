package com.etmisthefox.solacecore;

import com.etmisthefox.solacecore.commands.*;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.websocket.ModeratorWebSocketServer;
import com.etmisthefox.solacecore.websocket.ModCommandHandler;
import com.etmisthefox.solacecore.web.EmbeddedWebServer;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.solacecore.listeners.ChatListener;
import com.etmisthefox.solacecore.listeners.ConnectionListener;
import com.etmisthefox.solacecore.managers.DiscordPermissionManager;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.ChatInputUtil;
import com.etmisthefox.solacecore.utils.DisconnectScreenUtil;
import com.etmisthefox.solacecore.discord.DiscordManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public final class SolaceCore extends JavaPlugin {

    private Database database;
    private ModeratorWebSocketServer wsServer;
    private EmbeddedWebServer webServer;
    private LanguageManager lang;
    private DiscordPermissionManager discordPermissionManager;
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        discordPermissionManager = new DiscordPermissionManager(this);
        InventoryManager inventoryManager = new InventoryManager(this);
        inventoryManager.init();

        lang = new LanguageManager(this, getConfig().getString("language", "en"));
        DisconnectScreenUtil.init(getConfig(), lang);

        database = new Database(this);
        try {
            database.initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Database error...");
            getServer().shutdown();
            return; // Zastavit další inicializaci
        }

        if (getConfig().getBoolean("websocket.enabled", false)) {
            String wsHost = getConfig().getString("websocket.ip_address", "127.0.0.1");
            String wsPort = getConfig().getString("websocket.port", "8081");
            getLogger().info("=======================================================");
            getLogger().info("Starting Moderator WebSocket Server...");
            getLogger().info("Host: " + wsHost);
            getLogger().info("Port: " + wsPort);
            getLogger().info("URL: ws://" + wsHost + ":" + wsPort);
            getLogger().info("=======================================================");

            ModCommandHandler commandHandler = new ModCommandHandler(database, lang, this);
            wsServer = new ModeratorWebSocketServer(wsHost, wsPort, this, commandHandler, lang);
            try {
                wsServer.start();
                getLogger().info("WebSocket server STARTED on " + wsHost + ":" + wsPort);
            } catch (Exception e) {
                getLogger().severe("Failed to start WebSocket server: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().info("WebSocket server disabled in config.yml.");
        }

        if (getConfig().getBoolean("web.enabled", true)) {
            webServer = new EmbeddedWebServer(this, database);
            try {
                webServer.start();
            } catch (Exception e) {
                getLogger().severe("Failed to start embedded web server: " + e.getMessage());
            }
        }

        if (getConfig().getBoolean("discord_bot.enabled", false)) {
            try {
                discordManager = new DiscordManager(this, database, lang, discordPermissionManager);
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
        registerCommand("reload", new ReloadCommand(this, lang, discordPermissionManager));

        getServer().getPluginManager().registerEvents(new ConnectionListener(database, lang), this);
        getServer().getPluginManager().registerEvents(new ChatListener(database, lang), this);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        Command cmd = new Command(name) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                if (executor instanceof TabCompleter) {
                    List<String> completions = ((TabCompleter) executor).onTabComplete(sender, this, alias, args);
                    if (completions != null) {
                        return completions;
                    }
                }
                return super.tabComplete(sender, alias, args);
            }
        };

        // Automatické nastavení permisse podle názvu příkazu z paper-plugin.yml
        cmd.setPermission("solacecore." + name.toLowerCase());
        cmd.setPermissionMessage("You don't have permission to use this command.");

        // Zapsání do CommandMap
        getServer().getCommandMap().register(this.getName().toLowerCase(), cmd);
    }

    @Override
    public void onDisable() {
        ChatInputUtil.cancelAll();
        if (webServer != null) {
            webServer.stop();
        }
        if (database != null) {
            database.closeConnection();
        }
        if (wsServer != null) {
            try {
                wsServer.stop();
            } catch (Exception e) {
                getLogger().warning("Error stopping WebSocket server: " + e.getMessage());
            }
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
    }
}
