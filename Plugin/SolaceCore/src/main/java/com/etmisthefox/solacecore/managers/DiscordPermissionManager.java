package com.etmisthefox.solacecore.managers;

import com.etmisthefox.solacecore.SolaceCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;

public final class DiscordPermissionManager {

    private static final String DISCORD_PERMISSION_PREFIX = "solacecore.discord.";

    private final SolaceCore plugin;
    private final File permissionsFile;
    private FileConfiguration permissionsConfig;

    public DiscordPermissionManager(SolaceCore plugin) {
        this.plugin = plugin;
        this.permissionsFile = new File(plugin.getDataFolder(), "permissions.yml");
        reload();
    }

    public void reload() {
        if (!permissionsFile.exists()) {
            plugin.saveResource("permissions.yml", false);
        }
        permissionsConfig = YamlConfiguration.loadConfiguration(permissionsFile);
    }

    public String getRequiredPermission(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return null;
        }
        return DISCORD_PERMISSION_PREFIX + commandName.toLowerCase(Locale.ROOT);
    }

    public boolean hasPermission(String discordUserId, String requiredPermission) {
        if (discordUserId == null || discordUserId.isBlank() || requiredPermission == null || requiredPermission.isBlank()) {
            return false;
        }

        String required = requiredPermission.toLowerCase(Locale.ROOT);
        List<String> defaultPermissions = permissionsConfig.getStringList("discord.default_permissions");
        List<String> accountPermissions = permissionsConfig.getStringList("discord.accounts." + discordUserId);

        return containsPermission(defaultPermissions, required) || containsPermission(accountPermissions, required);
    }

    private boolean containsPermission(List<String> grantedPermissions, String requiredPermission) {
        for (String rawNode : grantedPermissions) {
            if (rawNode == null || rawNode.isBlank()) {
                continue;
            }

            String node = normalizeNode(rawNode);
            if ("*".equals(node) || "solacecore.*".equals(node) || (DISCORD_PERMISSION_PREFIX + "*").equals(node)) {
                return true;
            }
            if (requiredPermission.equals(node)) {
                return true;
            }
            if (node.endsWith(".*")) {
                String prefix = node.substring(0, node.length() - 1);
                if (requiredPermission.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalizeNode(String rawNode) {
        String node = rawNode.trim().toLowerCase(Locale.ROOT);
        if (node.isEmpty()) {
            return node;
        }
        if (!node.contains(".")) {
            return DISCORD_PERMISSION_PREFIX + node;
        }
        return node;
    }
}

