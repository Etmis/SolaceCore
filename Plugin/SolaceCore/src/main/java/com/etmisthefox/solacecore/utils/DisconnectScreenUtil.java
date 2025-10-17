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
                .append(Component.text(ColorAPI.colorize("Reason &8&l>> &7") + reason));
        if (time != null) {
            component = component
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text(ColorAPI.colorize("Duration &8&l>> &7") + time));
        }
        component = component
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Issued by &8&l>> &7" + operator));

        ConfigurationSection section = (fc != null) ? fc.getConfigurationSection("appeal_url") : null;
        if (section != null) {
            component = component
                .append(Component.newline())
                .append(Component.newline());
            Map<String, Object> appealUrl = section.getValues(false);
            for (Map.Entry<String, Object> entry : appealUrl.entrySet()) {
                String name = entry.getKey();
                String link = String.valueOf(entry.getValue());

                Component linkLine = Component.text(name + " >> ")
                        .append(Component.text(link)
                                .clickEvent(ClickEvent.openUrl(link))
                                .hoverEvent(HoverEvent.showText(Component.text("Open " + link))))
                        .append(Component.newline());
                component = component.append(linkLine);
            }
        }

        return component;
    }
}