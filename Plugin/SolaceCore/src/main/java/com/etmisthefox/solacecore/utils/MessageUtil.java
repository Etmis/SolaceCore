package com.etmisthefox.solacecore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    // Regex pro nalezení legacy hex barev (např. &#ff66cc)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Převede text obsahující jak Legacy formát tak MiniMessage do finálního Componentu.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 1. Převod legacy hex formátu (&#rrggbb) na MiniMessage hex (<#rrggbb>)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        String parsedText = sb.toString();

        // 2. Převod klasických ampersand kódů (&a, &b, &l atd.) na MiniMessage formát
        // Tímto zajistíme, že v textu zůstane pouze MiniMessage kompatibilní syntaxe.
        parsedText = parsedText.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");

        // 3. Finální parsování výsledného textu pomocí Kyori MiniMessage
        return MiniMessage.miniMessage().deserialize(parsedText);
    }
}