package com.etmisthefox.solacecore.utils;

import com.etmisthefox.solacecore.managers.LanguageManager;
import cz.foresttech.api.ColorAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class DisconnectScreenUtil {

    private static FileConfiguration fileConfiguration;
    private static LanguageManager languageManager;

    public static void init(FileConfiguration fc, LanguageManager lang) {
        fileConfiguration = fc;
        languageManager = lang;
    }

    public static Component formatDisconnectScreen(boolean kick, String punishmentMessage, String reason, String operator, String time) {
        String title = valueOrFallback(punishmentMessage, "You have been punished.");
        String reasonLabel = valueOrFallback(languageManager.getMessage("disconnect.reason_label"), "Reason");
        String durationLabel = valueOrFallback(languageManager.getMessage("disconnect.duration_label"), "Duration");
        String remainingLabel = valueOrFallback(languageManager.getMessage("disconnect.remaining_label"), "Remaining");
        String issuedByLabel = valueOrFallback(languageManager.getMessage("disconnect.issued_by_label"), "Issued by");
        String reasonValue = valueOrFallback(reason, "No reason specified");
        String operatorValue = valueOrFallback(operator, "System");

        Component component = Component.text(ColorAPI.colorize("&6&l[SolaceCore] &8&l>> &7") + title)
                .append(Component.newline())
                .append(Component.newline())
            .append(Component.text(ColorAPI.colorize("&6" + reasonLabel + " &8&l>> &7") + reasonValue));
        if (time != null) {
            if (kick) {
                component = component
                        .append(Component.newline())
                        .append(Component.newline())
                .append(Component.text(ColorAPI.colorize("&6" + durationLabel + " &8&l>> &7") + time));
            }
            else {
                component = component
                        .append(Component.newline())
                        .append(Component.newline())
                .append(Component.text(ColorAPI.colorize("&6" + remainingLabel + " &8&l>> &7") + time));
            }
        }
        component = component
                .append(Component.newline())
                .append(Component.newline())
            .append(Component.text(ColorAPI.colorize("&6" + issuedByLabel + " &8&l>> &7" + operatorValue)));

        ConfigurationSection section = fileConfiguration.getConfigurationSection("appeal_url");
        if (section != null) {
            component = component
                    .append(Component.newline())
                    .append(Component.newline());

            // Iterate entries under appeal_url
            Map<String, Object> entries = section.getValues(false);
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                String key = entry.getKey();

                String displayName = null;
                String link = null;

                ConfigurationSection sub = section.getConfigurationSection(key);
                if (sub != null) {
                    displayName = sub.getString("name", key);
                    link = sub.getString("link");
                }

                displayName = valueOrFallback(displayName, key);
                link = valueOrFallback(link, "");

                Component linkLine = Component.text(ColorAPI.colorize(displayName + " &8&l>> &7"))
                        .append(Component.text(ColorAPI.colorize(link)))
                        .append(Component.newline());
                component = component.append(linkLine);
            }
        }
        return component;
    }

    private static String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}