package com.etmisthefox.solacecore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private final FileConfiguration langConfig;

    public LanguageManager(Plugin plugin, String language) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "languages");

        if (!langFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            langFolder.mkdirs();
        }

        copyDefaultLanguages("en.yml");

        File langFile = new File(langFolder, language + ".yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void copyDefaultLanguages(String... languages) {
        for (String lang : languages) {
            File targetFile = new File(langFolder, lang);
            if (!targetFile.exists()) {
                saveResource("languages/" + lang, targetFile);
            }
        }
    }

    private void saveResource(String resourcePath, File outputFile) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream != null) {
                Files.copy(stream, outputFile.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String path) {
        return /*ColorAPI.colorize("{#FF8C00>}&l[SolaceCore]{#FFFFFF<}&r") + " " + */langConfig.getString(path);
    }

    public String getMessage(String path, String... placeholders) {
        String msg = getMessage(path);

        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }
}
