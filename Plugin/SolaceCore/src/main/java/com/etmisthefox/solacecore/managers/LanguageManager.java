package com.etmisthefox.solacecore.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

        copyAllLanguageFiles();

        File langFile = new File(langFolder, language + ".yml");
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
