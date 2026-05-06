package com.baymc.patrol.config;

import com.baymc.patrol.error.ErrorCode;
import com.baymc.patrol.error.PluginException;
import com.baymc.patrol.util.YamlDefaults;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Duration;
import java.util.function.BiConsumer;

/**
 * 配置管理器
 */
public final class ConfigManager {
    private final JavaPlugin plugin;
    private BiConsumer<String, Integer> completionLogger = (file, count) -> {
    };
    private volatile PluginSettings settings;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        completeMissingKeys();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        settings = read(config);
    }

    public void reload() {
        completeMissingKeys();
        plugin.reloadConfig();
        settings = read(plugin.getConfig());
    }

    public PluginSettings settings() {
        return settings;
    }

    public void setCompletionLogger(BiConsumer<String, Integer> completionLogger) {
        this.completionLogger = completionLogger;
    }

    private void completeMissingKeys() {
        int completedKeys = YamlDefaults.mergeMissingKeys(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));
        if (completedKeys > 0) {
            completionLogger.accept("config.yml", completedKeys);
        }
    }

    private PluginSettings read(FileConfiguration config) {
        String serverId = requireText(config, "server.id");
        String serverAlias = requireText(config, "server.alias");
        String keyPrefix = requireText(config, "redis.key-prefix");
        String velocityChannel = requireText(config, "velocity.channel");
        String language = requireText(config, "language.default");

        return new PluginSettings(
                new PluginSettings.ServerSettings(serverId, serverAlias),
                new PluginSettings.RedisSettings(
                        config.getBoolean("redis.enabled", true),
                        keyPrefix,
                        config.getString("redis.host", "127.0.0.1"),
                        config.getInt("redis.port", 6379),
                        config.getString("redis.password", ""),
                        config.getInt("redis.database", 0),
                        config.getBoolean("redis.ssl", false),
                        Duration.ofMillis(config.getLong("redis.timeout.connect-millis", 3000L)),
                        Duration.ofMillis(config.getLong("redis.timeout.command-millis", 3000L)),
                        config.getBoolean("redis.reconnect.enabled", true),
                        Duration.ofSeconds(config.getLong("redis.reconnect.delay-seconds", 5L))
                ),
                new PluginSettings.VelocitySettings(
                        config.getBoolean("velocity.enabled", true),
                        velocityChannel
                ),
                new PluginSettings.PatrolSettings(
                        config.getBoolean("patrol.same-server-first", false),
                        Duration.ofMinutes(config.getLong("patrol.session-expire-minutes", 60L)),
                        Duration.ofSeconds(config.getLong("patrol.pending-timeout-seconds", 30L)),
                        Duration.ofSeconds(config.getLong("patrol.active-timeout-seconds", 30L))
                ),
                new PluginSettings.TeleportSettings(
                        config.getInt("teleport.delay-after-join-ticks", 10)
                ),
                new PluginSettings.SyncSettings(
                        config.getInt("sync.online-refresh-seconds", 5),
                        Duration.ofSeconds(config.getLong("sync.online-ttl-seconds", 15L)),
                        Duration.ofSeconds(config.getLong("sync.heartbeat-ttl-seconds", 15L))
                ),
                new PluginSettings.LanguageSettings(language),
                new PluginSettings.RuntimeSettings(
                        new PluginSettings.ErrorSettings(
                                config.getBoolean("runtime.error.debug", false),
                                config.getBoolean("runtime.error.print-stacktrace-to-console", false),
                                config.getBoolean("runtime.error.show-trace-id-to-player", true)
                        )
                )
        );
    }

    private String requireText(FileConfiguration config, String path) {
        String value = config.getString(path, "");
        if (value == null || value.isBlank()) {
            throw new PluginException(ErrorCode.CONFIG_INVALID, "Missing config value: " + path);
        }
        return value;
    }
}
