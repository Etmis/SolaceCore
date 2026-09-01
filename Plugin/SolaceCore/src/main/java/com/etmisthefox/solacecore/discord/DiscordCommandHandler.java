package com.etmisthefox.solacecore.discord;

import com.etmisthefox.solacecore.SolaceCore;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.DiscordPermissionManager;
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

    private final SolaceCore plugin;
    private final Database database;
    private final LanguageManager lang;
    private final DiscordPermissionManager discordPermissionManager;

    public DiscordCommandHandler(SolaceCore plugin, Database database, LanguageManager lang, DiscordPermissionManager discordPermissionManager) {
        this.plugin = plugin;
        this.database = database;
        this.lang = lang;
        this.discordPermissionManager = discordPermissionManager;
    }

    public void registerCommands(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("ban", lang.getRawMessage("discord.commands.ban.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.ban"), false),
                Commands.slash("unban", lang.getRawMessage("discord.commands.unban.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true),
                Commands.slash("kick", lang.getRawMessage("discord.commands.kick.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.kick"), false),
                Commands.slash("mute", lang.getRawMessage("discord.commands.mute.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.mute"), false),
                Commands.slash("unmute", lang.getRawMessage("discord.commands.unmute.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true),
                Commands.slash("warn", lang.getRawMessage("discord.commands.warn.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.warn"), false),
                Commands.slash("tempban", lang.getRawMessage("discord.commands.tempban.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getRawMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.ban"), false),
                Commands.slash("tempmute", lang.getRawMessage("discord.commands.tempmute.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getRawMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.mute"), false),
                Commands.slash("ipban", lang.getRawMessage("discord.commands.ipban.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.ipban"), false),
                Commands.slash("tempipban", lang.getRawMessage("discord.commands.tempipban.description"))
                        .addOption(OptionType.STRING, "player", lang.getRawMessage("discord.commands.option.player"), true)
                        .addOption(OptionType.STRING, "duration", lang.getRawMessage("discord.commands.option.duration"), true)
                        .addOption(OptionType.STRING, "reason", lang.getRawMessage("discord.commands.option.reason.ipban"), false)
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
            event.reply(lang.getRawMessage("discord.reply.cannot_determine_user")).setEphemeral(true).queue();
            return;
        }

        String requiredPermission = "solacecore." + commandName;
        if (!discordPermissionManager.hasPermission(member.getId(), requiredPermission)) {
            event.reply(lang.getRawMessage("discord.reply.no_permission")).setEphemeral(true).queue();
            return;
        }

        // Změna: Získáváme pouze jméno uživatele z Discordu jako String
        String operatorName = member.getUser().getName();

        var playerOption = event.getOption("player");
        if (playerOption == null) {
            event.reply(lang.getRawMessage("discord.reply.player_name_required")).setEphemeral(true).queue();
            return;
        }

        String playerName = playerOption.getAsString();
        var reasonOption = event.getOption("reason");
        String reason = reasonOption != null ? reasonOption.getAsString() : lang.getRawMessage("punishment.no_reason");

        event.deferReply(true).queue();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Player target = Bukkit.getPlayerExact(playerName);

                switch (commandName) {
                    case "ban" -> {
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.BAN, operatorName, playerName, reason, null);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.ban_success", "player", playerName)).queue();
                    }
                    case "unban" -> {
                        database.unpunishPlayer(playerName, "ban");
                        database.unpunishPlayer(playerName, "tempban");
                        database.unpunishPlayer(playerName, "ipban");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNBAN", operatorName, playerName, "Unbanned via Discord", null);
                        }
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.unban_success", "player", playerName)).queue();
                    }
                    case "kick" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getRawMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.KICK, operatorName, playerName, reason, null);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.kick_success", "player", playerName)).queue();
                    }
                    case "mute" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getRawMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.MUTE, operatorName, playerName, reason, null);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.mute_success", "player", playerName)).queue();
                    }
                    case "unmute" -> {
                        database.unpunishPlayer(playerName, "mute");
                        database.unpunishPlayer(playerName, "tempmute");
                        DiscordManager dm = DiscordManager.getInstance();
                        if (dm != null) {
                            dm.logActionToDiscord("UNMUTE", operatorName, playerName, "Unmuted via Discord", null);
                        }
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.unmute_success", "player", playerName)).queue();
                    }
                    case "warn" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getRawMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.WARN, operatorName, playerName, reason, null);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.warn_success", "player", playerName)).queue();
                    }
                    case "tempban" -> {
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPBAN, operatorName, playerName, reason, durationSeconds);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.tempban_success", "player", playerName)).queue();
                    }
                    case "tempmute" -> {
                        if (target == null) {
                            event.getHook().sendMessage(lang.getRawMessage("discord.reply.player_not_online", "player", playerName)).queue();
                            return;
                        }
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1h";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPMUTE, operatorName, playerName, reason, durationSeconds);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.tempmute_success", "player", playerName)).queue();
                    }
                    case "ipban" -> {
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.IPBAN, operatorName, playerName, reason, null);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.ipban_success", "player", playerName)).queue();
                    }
                    case "tempipban" -> {
                        var durationOption = event.getOption("duration");
                        String duration = durationOption != null ? durationOption.getAsString() : "1d";
                        Long durationSeconds = TimeUtil.parseDuration(duration);
                        PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPIPBAN, operatorName, playerName, reason, durationSeconds);
                        event.getHook().sendMessage(lang.getRawMessage("discord.reply.tempipban_success", "player", playerName)).queue();
                    }
                    default -> event.getHook().sendMessage(lang.getRawMessage("discord.reply.unknown_command", "command", commandName)).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage(lang.getRawMessage("discord.reply.database_error", "error", e.getMessage())).queue();
                Logger.getLogger(DiscordCommandHandler.class.getName()).log(Level.SEVERE, "Discord command error", e);
            }
        });
    }
}
