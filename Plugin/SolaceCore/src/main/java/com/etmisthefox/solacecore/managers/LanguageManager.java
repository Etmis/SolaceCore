package com.etmisthefox.solacecore.managers;

import com.etmisthefox.solacecore.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarFile;

public final class LanguageManager {

    private final Plugin plugin;
    private final File langFolder;
    private FileConfiguration langConfig;
    private String activeLanguage;

    public LanguageManager(Plugin plugin, String language) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "languages");
        this.activeLanguage = language;

        if (!langFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            langFolder.mkdirs();
        }

        reload();
    }

    public void reload() {
        copyAllLanguageFiles();

        String language = plugin.getConfig().getString("language", activeLanguage != null ? activeLanguage : "en");
        activeLanguage = language;

        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()) {
            File fallback = new File(langFolder, "en.yml");
            if (fallback.exists()) {
                langFile = fallback;
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void copyAllLanguageFiles() {
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    jar.stream()
                            .filter(entry -> entry.getName().startsWith("languages/") && entry.getName().endsWith(".yml"))
                            .forEach(entry -> {
                                String fileName = entry.getName().substring("languages/".length());
                                File targetFile = new File(langFolder, fileName);
                                if (!targetFile.exists()) {
                                    saveResource(entry.getName(), targetFile);
                                }
                            });
                }
            } else {
                // Running in IDE - copy from resources directory
                File resourcesDir = new File(plugin.getClass().getClassLoader().getResource("languages").toURI());
                File[] files = resourcesDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files != null) {
                    for (File file : files) {
                        File targetFile = new File(langFolder, file.getName());
                        if (!targetFile.exists()) {
                            saveResource("languages/" + file.getName(), targetFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Získá zprávu z configu a vrátí ji jako naformátovaný Component s prefixem.
     */
    public Component getMessage(String path) {
        String rawMessage = langConfig.getString(path);

        if (rawMessage == null || rawMessage.isEmpty()) {
            return Component.empty();
        }

        return MessageUtil.parse(rawMessage);
    }

    /**
     * Získá zprávu z configu, nahradí placeholdery a vrátí jako naformátovaný Component.
     */
    public Component getMessage(String path, String... placeholders) {
        String msg = langConfig.getString(path);

        if (msg == null || msg.isEmpty()) {
            return Component.empty();
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }

        return MessageUtil.parse(msg);
    }

    public String getRawMessage(String path) {
        return langConfig.getString(path);
    }

    public String getRawMessage(String path, String... placeholders) {
        String msg = langConfig.getString(path);

        for (int i = 0; i < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }

        return msg;
    }
}