package com.etmisthefox.solacecore.utils;

import cz.foresttech.api.ColorAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public final class DisconnectScreenUtil {

    public static void setFc(FileConfiguration fc) {
        DisconnectScreenUtil.fc = fc;
    }

    private static FileConfiguration fc;

    public static Component formatDisconnectScreen(String punishmentMessage, String reason, String operator, String time) {
        Component component = Component.text(ColorAPI.colorize("&6&l[SolaceCore] &8&l>> &7") + punishmentMessage)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(ColorAPI.colorize("&6Reason &8&l>> &7") + reason));
        if (time != null) {
            component = component
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text(ColorAPI.colorize("&6Duration &8&l>> &7") + time));
        }
        component = component
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(ColorAPI.colorize("&6Issued by &8&l>> &7" + operator)));

        // Read appeal links from config (supports both new and legacy structure)
        if (fc != null) {
            ConfigurationSection section = fc.getConfigurationSection("appeal_url");
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

                    // New structure: subsection with name + link
                    ConfigurationSection sub = section.getConfigurationSection(key);
                    if (sub != null) {
                        displayName = sub.getString("name", key);
                        link = sub.getString("link");
                    } else {
                        // Legacy structure: key -> direct URL string
                        Object value = entry.getValue();
                        if (value != null) {
                            displayName = key;
                            link = String.valueOf(value);
                        }
                    }

                    if (link == null || link.isBlank()) {
                        continue; // skip invalid/missing link
                    }

                    if (displayName == null || displayName.isBlank()) {
                        displayName = key;
                    }

                    String visibleLinkText = ColorAPI.colorize(link);
                    if (visibleLinkText.isEmpty()) continue;

                    Component linkLine = Component.text(ColorAPI.colorize(displayName + " &8&l>> &7"))
                            .append(Component.text(visibleLinkText))
                            .append(Component.newline());
                    component = component.append(linkLine);
                }
            }
        }
        return component;
    }
}