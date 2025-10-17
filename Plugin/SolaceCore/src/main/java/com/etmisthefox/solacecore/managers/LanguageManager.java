package com.etmisthefox.solacecore.managers;

import cz.foresttech.api.ColorAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final FileConfiguration langConfig;

    public LanguageManager(Plugin plugin, String language) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "languages");

        // Vytvoří složku languages pokud neexistuje
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Zkopíruje všechny jazyky ze složky resources/lang do plugins/Xban/languages/
        copyDefaultLanguages("cs.yml", "en.yml");

        // Načte vybraný jazyk
        File langFile = new File(langFolder, language + ".yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void copyDefaultLanguages(String... languages) {
        for (String lang : languages) {
            File targetFile = new File(langFolder, lang);
            if (!targetFile.exists()) {
                saveResource("lang/" + lang, targetFile);
            }
        }
    }

    private void saveResource(String resourcePath, File outputFile) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream != null) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                config.save(outputFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String path) {
        return /*ColorAPI.colorize("{#FF8C00>}&l[SolaceCore]{#FFFFFF<}&r") + " " + */langConfig.getString("messages." + path);
    }

    public String getMessage(String path, String... placeholders) {
        String msg = getMessage(path);

        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }
}
