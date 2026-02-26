package com.etmisthefox.solacecore.discord;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
import com.etmisthefox.solacecore.utils.TimeUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.JDA;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordCommandHandler extends ListenerAdapter {

    private final Database database;
    private final LanguageManager lang;

    public DiscordCommandHandler(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    public void registerCommands(JDA jda) {
        // Using updateCommands() to batch update all commands at once
        jda.updateCommands().addCommands(
                Commands.slash("ban", "Ban a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for ban", false),
                Commands.slash("unban", "Unban a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true),
                Commands.slash("kick", "Kick a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for kick", false),
                Commands.slash("mute", "Mute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for mute", false),
                Commands.slash("unmute", "Unmute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true),
                Commands.slash("warn", "Warn a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for warning", false),
                Commands.slash("tempban", "Temporarily ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for ban", false),
                Commands.slash("tempmute", "Temporarily mute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for mute", false),
                Commands.slash("ipban", "IP ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for IP ban", false),
                Commands.slash("tempipban", "Temporarily IP ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for IP ban", false)
        ).queue(
                success -> System.out.println("✅ Successfully registered " + success.size() + " slash commands!"),
                error -> System.err.println("❌ Failed to register commands: " + error.getMessage())
        );
    }

    public void registerGuildCommands(JDA jda, String guildId) {
        // Guild-specific commands update instantly (for testing)
        var guild = jda.getGuildById(guildId);
        if (guild == null) {
            System.err.println("❌ Guild with ID " + guildId + " not found!");
            return;
        }

        guild.updateCommands().addCommands(
                Commands.slash("ban", "Ban a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for ban", false),
                Commands.slash("unban", "Unban a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true),
                Commands.slash("kick", "Kick a player from the server")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for kick", false),
                Commands.slash("mute", "Mute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for mute", false),
                Commands.slash("unmute", "Unmute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true),
                Commands.slash("warn", "Warn a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for warning", false),
                Commands.slash("tempban", "Temporarily ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for ban", false),
                Commands.slash("tempmute", "Temporarily mute a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for mute", false),
                Commands.slash("ipban", "IP ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "reason", "Reason for IP ban", false),
                Commands.slash("tempipban", "Temporarily IP ban a player")
                        .addOption(OptionType.STRING, "player", "Player name", true)
                        .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d, 24h, 30m)", true)
                        .addOption(OptionType.STRING, "reason", "Reason for IP ban", false)
        ).queue(
                success -> System.out.println("✅ Successfully registered " + success.size() + " guild-specific slash commands!"),
                error -> System.err.println("❌ Failed to register guild commands: " + error.getMessage())
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        Member member = event.getMember();

        if (member == null) {
            event.reply("❌ Error: Cannot determine user.").setEphemeral(true).queue();
            return;
        }

        String operator = member.getEffectiveName();
        var playerOption = event.getOption("player");
        if (playerOption == null) {
            event.reply("❌ Player name is required.").setEphemeral(true).queue();
            return;
        }

        String playerName = playerOption.getAsString();
        var reasonOption = event.getOption("reason");
        String reason = reasonOption != null ? reasonOption.getAsString() : "No reason provided";

        // Defer reply immediately
        event.deferReply(true).queue();

        // Run on main server thread
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SolaceCore"), () -> {
            try {
                Player target = Bukkit.getPlayer(playerName);

                switch (commandName) {
                    case "ban" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.BAN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been banned.").queue();
                    }
                    case "unban" -> {
                        database.unpunishPlayer(playerName, "ban");
                        database.unpunishPlayer(playerName, "tempban");
                        database.unpunishPlayer(playerName, "ipban");
                        database.logAction("UNBAN", operator, playerName, "Unbanned via Discord", "discord");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNBAN", operator, playerName, "Unbanned via Discord", null);
                        }
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been unbanned.").queue();
                    }
                    case "kick" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.KICK, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been kicked.").queue();
                    }
                    case "mute" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.MUTE, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been muted.").queue();
                    }
                    case "unmute" -> {
                        database.unpunishPlayer(playerName, "mute");
                        database.unpunishPlayer(playerName, "tempmute");
                        database.logAction("UNMUTE", operator, playerName, "Unmuted via Discord", "discord");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNMUTE", operator, playerName, "Unmuted via Discord", null);
                        }
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been unmuted.").queue();
                    }
                    case "warn" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.WARN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been warned.").queue();
                    }
                    case "tempban" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPBAN, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been temporarily banned.").queue();
                    }
                    case "tempmute" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1h";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPMUTE, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been temporarily muted.").queue();
                    }
                    case "ipban" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.IPBAN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been IP banned.").queue();
                    }
                    case "tempipban" -> {
                        if (target == null) {
                            event.getHook().sendMessage("❌ Player **" + playerName + "** is not online.").queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPIPBAN, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage("✅ Player **" + playerName + "** has been temporarily IP banned.").queue();
                    }
                    default -> event.getHook().sendMessage("❌ Unknown command: " + commandName).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage("❌ Database error: " + e.getMessage()).queue();
                Logger.getLogger(DiscordCommandHandler.class.getName()).log(Level.SEVERE, "Discord command error", e);
            }
        });
    }
}

