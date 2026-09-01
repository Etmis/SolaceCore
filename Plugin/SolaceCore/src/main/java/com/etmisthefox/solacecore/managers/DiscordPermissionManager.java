package com.etmisthefox.solacecore.managers;

import com.etmisthefox.solacecore.SolaceCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;

public final class DiscordPermissionManager {

    private final SolaceCore plugin;
    private final File permissionsFile;
    private FileConfiguration permissionsConfig;

    public DiscordPermissionManager(SolaceCore plugin) {
        this.plugin = plugin;
        this.permissionsFile = new File(plugin.getDataFolder(), "discord.yml");
        reload();
    }

    public void reload() {
        if (!permissionsFile.exists()) {
            plugin.saveResource("discord.yml", false);
        }
        permissionsConfig = YamlConfiguration.loadConfiguration(permissionsFile);
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

            String node = rawNode.trim().toLowerCase(Locale.ROOT);
            if ("*".equals(node) || "solacecore.*".equals(node)) {
                return true;
            }
            if (requiredPermission.equals(node)) {
                return true;
            }
        }
        return false;
    }
}
