package com.etmisthefox.solacecore.discord;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.InputStream;
import java.net.URL;

public final class DiscordManager {

    private static DiscordManager instance;
    private final SolaceCore plugin;
    private final Database database;
    private final LanguageManager lang;
    private JDA jda;
    private TextChannel logsChannel;

    public DiscordManager(SolaceCore plugin, Database database, LanguageManager lang) {
        this.plugin = plugin;
        this.database = database;
        this.lang = lang;
        DiscordManager.instance = this;
    }

    public static DiscordManager getInstance() {
        return instance;
    }

    public void initialize() throws InterruptedException {
        String token = plugin.getConfig().getString("discord_bot.token");
        String logsChannelId = plugin.getConfig().getString("discord_bot.logs_channel_id");
        String botName = plugin.getConfig().getString("discord_bot.bot_username");
        String botAvatarUrl = plugin.getConfig().getString("discord_bot.bot_avatar_url");

        jda = JDABuilder.create(token, net.dv8tion.jda.api.requests.GatewayIntent.getIntents(0)).build().awaitReady();

        // Set bot name if configured
        if (botName != null && !botName.isEmpty()) {
            jda.getSelfUser().getManager().setName(botName).queue(
                success -> plugin.getLogger().info("Discord bot name set to: " + botName),
                error -> plugin.getLogger().warning("Failed to set bot name: " + error.getMessage())
            );
        }

        // Set bot avatar if configured
        if (botAvatarUrl != null && !botAvatarUrl.isEmpty()) {
            try {
                URL url = new URL(botAvatarUrl);
                InputStream inputStream = url.openStream();
                Icon icon = Icon.from(inputStream);
                jda.getSelfUser().getManager().setAvatar(icon).queue(
                    success -> plugin.getLogger().info("Discord bot avatar updated from URL: " + botAvatarUrl),
                    error -> plugin.getLogger().warning("Failed to set bot avatar: " + error.getMessage())
                );
                inputStream.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load bot avatar from URL: " + botAvatarUrl + " - " + e.getMessage());
            }
        }

        if (logsChannelId != null && !logsChannelId.isEmpty()) {
            logsChannel = jda.getTextChannelById(logsChannelId);
            if (logsChannel == null) {
                plugin.getLogger().warning("Discord logs channel with ID " + logsChannelId + " not found!");
            }
        }

        DiscordCommandHandler commandHandler = new DiscordCommandHandler(database, lang);

        jda.addEventListener(commandHandler);
    }

    public void logActionToDiscord(String actionType, String operator, String targetPlayer, String reason, String duration) {
        if (logsChannel == null) {
            return;
        }

        String embed = formatActionLog(actionType, operator, targetPlayer, reason, duration);
        logsChannel.sendMessage(embed).queue();
    }

    private String formatActionLog(String actionType, String operator, String targetPlayer, String reason, String duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(actionType).append("**\n");
        sb.append("**Operator:** ").append(operator).append("\n");
        sb.append("**Player:** ").append(targetPlayer).append("\n");
        sb.append("**Reason:** ").append(reason != null ? reason : "N/A").append("\n");
        if (duration != null) {
            sb.append("**Duration:** ").append(duration).append("\n");
        }
        sb.append("**Time:** <t:").append(System.currentTimeMillis() / 1000).append(":R>\n");
        return sb.toString();
    }

    public JDA getJDA() {
        return jda;
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}

