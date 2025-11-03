package com.etmisthefox.solacecore.managers;

import com.etmisthefox.solacecore.enums.PunishmentType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PermissionManager {

    // Základní uzly
    public static final String ADMIN_PERMISSION = "solacecore.admin";
    public static final String BYPASS_PERMISSION = "solacecore.bypass"; // volitelný, pokud chcete zvláštní bypass mimo OP/admin

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

    // --- Admin/bypass ---

    public boolean hasAdminPermission(CommandSender sender) {
        // Konzole = admin, hráč s OP nebo s admin uzlem = admin
        if (!(sender instanceof Player player)) return true;
        return player.isOp() || sender.hasPermission(ADMIN_PERMISSION);
    }

    public boolean canBypassRestrictions(CommandSender sender) {
        return hasAdminPermission(sender) || sender.hasPermission(BYPASS_PERMISSION);
    }

    // --- Příkazy ---

    public boolean canUseCommand(CommandSender sender, String commandType) {
        if (canBypassRestrictions(sender)) return true;
        String node = getCommandPermission(commandType);
        return node != null && sender.hasPermission(node);
    }

    public String getCommandPermission(String commandType) {
        if (commandType == null) return null;
        switch (commandType.toLowerCase()) {
            case "kick":
                return COMMAND_KICK;
            case "ban":
                return COMMAND_BAN;
            case "ipban":
                return COMMAND_IPBAN;
            case "tempban":
                return COMMAND_TEMPBAN;
            case "tempipban":
                return COMMAND_TEMPIPBAN;
            case "unban":
                return COMMAND_UNBAN;
            case "mute":
                return COMMAND_MUTE;
            case "tempmute":
                return COMMAND_TEMPMUTE;
            case "unmute":
                return COMMAND_UNMUTE;
            case "warn":
                return COMMAND_WARN;
            case "warns":
                return COMMAND_WARNS;
            case "menu":
                return COMMAND_MENU;
            default:
                return null;
        }
    }

    // --- Ochrany cílů ---

    public boolean hasProtection(Player target, PunishmentType type) {
        if (target == null) return false;
        // OP hráče považuj jako chráněné implicitně
        if (target.isOp()) return true;

        return switch (type) {
            case KICK -> target.hasPermission(PROTECT_KICK);
            case BAN, TEMPBAN, IPBAN -> target.hasPermission(PROTECT_BAN);
            case MUTE, TEMPMUTE -> target.hasPermission(PROTECT_MUTE);
            default -> false;
        };
    }
}
