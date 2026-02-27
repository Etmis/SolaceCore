package com.etmisthefox.solacecore.managers;

import com.etmisthefox.solacecore.enums.PunishmentType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PermissionManager {

    // Příkazová oprávnění (podle plugin.yml)
    public static final String COMMAND_KICK = "solacecore.kick";
    public static final String COMMAND_BAN = "solacecore.ban";
    public static final String COMMAND_IPBAN = "solacecore.ipban";
    public static final String COMMAND_TEMPBAN = "solacecore.tempban";
    public static final String COMMAND_TEMPIPBAN = "solacecore.tempipban";
    public static final String COMMAND_UNBAN = "solacecore.unban";
    public static final String COMMAND_MUTE = "solacecore.mute";
    public static final String COMMAND_TEMPMUTE = "solacecore.tempmute";
    public static final String COMMAND_UNMUTE = "solacecore.unmute";
    public static final String COMMAND_WARN = "solacecore.warn";
    public static final String COMMAND_WARNS = "solacecore.warns";
    public static final String COMMAND_MENU = "solacecore.menu";

    // Ochranné uzly (cílové hráče nelze potrestat danou akcí)
    public static final String PROTECT_BAN = "solacecore.banprotection";
    public static final String PROTECT_KICK = "solacecore.kickprotection";
    public static final String PROTECT_MUTE = "solacecore.muteprotection";

    // --- Příkazy ---

    public boolean canUseCommand(CommandSender sender, String commandType) {
        String node = getCommandPermission(commandType);
        return node != null && sender.hasPermission(node);
    }

    public String getCommandPermission(String commandType) {
        if (commandType == null) return null;
        return switch (commandType.toLowerCase()) {
            case "kick" -> COMMAND_KICK;
            case "ban" -> COMMAND_BAN;
            case "ipban" -> COMMAND_IPBAN;
            case "tempban" -> COMMAND_TEMPBAN;
            case "tempipban" -> COMMAND_TEMPIPBAN;
            case "unban" -> COMMAND_UNBAN;
            case "mute" -> COMMAND_MUTE;
            case "tempmute" -> COMMAND_TEMPMUTE;
            case "unmute" -> COMMAND_UNMUTE;
            case "warn" -> COMMAND_WARN;
            case "warns" -> COMMAND_WARNS;
            case "menu" -> COMMAND_MENU;
            default -> null;
        };
    }

    // --- Ochrany cílů ---

    public boolean hasProtection(Player target, PunishmentType type) {
        if (target == null) return false;

        return switch (type) {
            case KICK -> target.hasPermission(PROTECT_KICK);
            case BAN, TEMPBAN, IPBAN -> target.hasPermission(PROTECT_BAN);
            case MUTE, TEMPMUTE -> target.hasPermission(PROTECT_MUTE);
            default -> false;
        };
    }
}
