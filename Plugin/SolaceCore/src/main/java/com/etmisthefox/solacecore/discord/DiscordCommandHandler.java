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

public final class DiscordCommandHandler extends ListenerAdapter {

    private final Database database;
    private final LanguageManager lang;

    public DiscordCommandHandler(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    public void registerCommands(JDA jda) {
        // Using updateCommands() to batch update all commands at once
        jda.updateCommands().addCommands(
                Commands.slash("ban", lang.getMessage("discord.commands.ban.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.ban"), false),
                Commands.slash("unban", lang.getMessage("discord.commands.unban.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true),
                Commands.slash("kick", lang.getMessage("discord.commands.kick.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.kick"), false),
                Commands.slash("mute", lang.getMessage("discord.commands.mute.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.mute"), false),
                Commands.slash("unmute", lang.getMessage("discord.commands.unmute.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true),
                Commands.slash("warn", lang.getMessage("discord.commands.warn.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.warn"), false),
                Commands.slash("tempban", lang.getMessage("discord.commands.tempban.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.ban"), false),
                Commands.slash("tempmute", lang.getMessage("discord.commands.tempmute.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.mute"), false),
                Commands.slash("ipban", lang.getMessage("discord.commands.ipban.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.ipban"), false),
                Commands.slash("tempipban", lang.getMessage("discord.commands.tempipban.description"))
                        .addOption(OptionType.STRING, "player", lang.getMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getMessage("discord.commands.option.reason.ipban"), false)
        ).queue(
                success -> System.out.println("✅ Successfully registered " + success.size() + " slash commands!"),
                error -> System.err.println("❌ Failed to register commands: " + error.getMessage())
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        Member member = event.getMember();

        if (member == null) {
            event.reply(lang.getMessage("discord.reply.cannot_determine_user")).setEphemeral(true).queue();
            return;
        }

        String operator = member.getEffectiveName();
        var playerOption = event.getOption("player");
        if (playerOption == null) {
            event.reply(lang.getMessage("discord.reply.player_name_required")).setEphemeral(true).queue();
            return;
        }

        String playerName = playerOption.getAsString();
        var reasonOption = event.getOption("reason");
        String reason = reasonOption != null ? reasonOption.getAsString() : lang.getMessage("punishment.no_reason");

        // Defer reply immediately
        event.deferReply(true).queue();

        // Run on main server thread
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SolaceCore"), () -> {
            try {
                Player target = Bukkit.getPlayer(playerName);

                switch (commandName) {
                    case "ban" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.BAN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.ban_success", "player", playerName)).queue();
                    }
                    case "unban" -> {
                        database.unpunishPlayer(playerName, "ban");
                        database.unpunishPlayer(playerName, "tempban");
                        database.unpunishPlayer(playerName, "ipban");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNBAN", operator, playerName, "Unbanned via Discord", null);
                        }
                        event.getHook().sendMessage(lang.getMessage("discord.reply.unban_success", "player", playerName)).queue();
                    }
                    case "kick" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.KICK, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.kick_success", "player", playerName)).queue();
                    }
                    case "mute" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.MUTE, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.mute_success", "player", playerName)).queue();
                    }
                    case "unmute" -> {
                        database.unpunishPlayer(playerName, "mute");
                        database.unpunishPlayer(playerName, "tempmute");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNMUTE", operator, playerName, "Unmuted via Discord", null);
                        }
                        event.getHook().sendMessage(lang.getMessage("discord.reply.unmute_success", "player", playerName)).queue();
                    }
                    case "warn" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.WARN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.warn_success", "player", playerName)).queue();
                    }
                    case "tempban" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPBAN, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.tempban_success", "player", playerName)).queue();
                    }
                    case "tempmute" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1h";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPMUTE, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.tempmute_success", "player", playerName)).queue();
                    }
                    case "ipban" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.IPBAN, Bukkit.getConsoleSender(), target, reason, null, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.ipban_success", "player", playerName)).queue();
                    }
                    case "tempipban" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPIPBAN, Bukkit.getConsoleSender(), target, reason, durationSeconds, "discord");
                        event.getHook().sendMessage(lang.getMessage("discord.reply.tempipban_success", "player", playerName)).queue();
                    }
                    default -> event.getHook().sendMessage(lang.getMessage("discord.reply.unknown_command", "command", commandName)).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage(lang.getMessage("discord.reply.database_error", "error", e.getMessage())).queue();
                Logger.getLogger(DiscordCommandHandler.class.getName()).log(Level.SEVERE, "Discord command error", e);
            }
        });
    }
}

