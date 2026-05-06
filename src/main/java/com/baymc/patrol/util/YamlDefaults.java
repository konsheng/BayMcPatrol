package com.baymc.patrol.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * YAML 默认键补全工具
 */
public final class YamlDefaults {
    private YamlDefaults() {
    }

    public static int mergeMissingKeys(JavaPlugin plugin, String resourcePath, File targetFile) {
        if (!targetFile.exists()) {
            plugin.saveResource(resourcePath, false);
            return 0;
        }

        YamlConfiguration defaults = loadResource(plugin, resourcePath);
        YamlConfiguration current = loadFile(targetFile);
        int completedKeys = 0;

        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || current.contains(key)) {
                continue;
            }
            current.set(key, defaults.get(key));
            completedKeys++;
        }

        if (completedKeys <= 0) {
            return 0;
        }

        try {
            current.save(targetFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save completed YAML file: " + targetFile.getName());
        }
        return completedKeys;
    }

    private static YamlConfiguration loadResource(JavaPlugin plugin, String resourcePath) {
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.options().parseComments(true);
            configuration.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load resource YAML: " + resourcePath, exception);
        }
    }

    private static YamlConfiguration loadFile(File file) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.options().parseComments(true);
            configuration.load(file);
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Failed to load YAML file: " + file.getName(), exception);
        }
    }
}
