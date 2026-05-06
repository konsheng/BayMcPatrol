package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.config.PluginSettings;
import com.baymc.patrol.error.ErrorCode;
import com.baymc.patrol.error.ErrorReport;
import com.baymc.patrol.error.ErrorService;
import com.baymc.patrol.error.PluginException;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Redis 连接管理器
 */
public final class RedisManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final ErrorService errorService;
    private final SchedulerAdapter scheduler;
    private volatile RedisClient client;
    private volatile StatefulRedisConnection<String, String> connection;
    private volatile boolean available;
    private volatile long lastReconnectAttempt;

    public RedisManager(
            JavaPlugin plugin,
            ConfigManager configManager,
            LanguageService languageService,
            ErrorService errorService,
            SchedulerAdapter scheduler
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageService = languageService;
        this.errorService = errorService;
        this.scheduler = scheduler;
    }

    public synchronized void connect() {
        connectInternal(true);
    }

    private synchronized boolean connectInternal(boolean logFailure) {
        close();
        PluginSettings.RedisSettings settings = configManager.settings().redis();
        if (!settings.enabled()) {
            available = false;
            return false;
        }
        try {
            RedisURI.Builder builder = RedisURI.builder()
                    .withHost(settings.host())
                    .withPort(settings.port())
                    .withDatabase(settings.database())
                    .withSsl(settings.ssl())
                    .withTimeout(settings.commandTimeout());
            if (settings.password() != null && !settings.password().isBlank()) {
                builder.withPassword(settings.password().toCharArray());
            }
            client = RedisClient.create(builder.build());
            client.setOptions(ClientOptions.builder().autoReconnect(settings.reconnectEnabled()).build());
            connection = client.connect();
            connection.setTimeout(settings.commandTimeout());
            connection.sync().ping();
            available = true;
            plugin.getServer().getConsoleSender().sendMessage(languageService.component("console.redis-connected"));
            return true;
        } catch (Exception exception) {
            available = false;
            if (logFailure) {
                ErrorReport report = errorService.report(toCode(exception), exception);
                plugin.getServer().getConsoleSender().sendMessage(languageService.component(
                        "console.redis-connect-failed",
                        MessagePlaceholder.unparsed("trace_id", report.traceId())
                ));
            }
            close();
            return false;
        }
    }

    public synchronized void close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (client != null) {
            client.shutdown();
            client = null;
        }
        available = false;
    }

    public boolean isEnabled() {
        return configManager.settings().redis().enabled();
    }

    public boolean isAvailable() {
        return isEnabled() && available && connection != null && connection.isOpen();
    }

    public <T> CompletableFuture<T> execute(Function<RedisCommands<String, String>, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                if (!ensureConnection()) {
                    throw new PluginException(ErrorCode.REDIS_UNAVAILABLE, "Redis unavailable");
                }
                T result = function.apply(connection.sync());
                available = true;
                future.complete(result);
            } catch (Exception exception) {
                available = false;
                errorService.report(toCode(exception), exception);
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private synchronized boolean ensureConnection() {
        if (!isEnabled()) {
            return false;
        }
        if (connection != null && connection.isOpen() && available) {
            return true;
        }
        PluginSettings.RedisSettings settings = configManager.settings().redis();
        if (!settings.reconnectEnabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastReconnectAttempt < settings.reconnectDelay().toMillis()) {
            return false;
        }
        lastReconnectAttempt = now;
        return connectInternal(false);
    }

    private ErrorCode toCode(Throwable throwable) {
        if (throwable instanceof RedisCommandTimeoutException) {
            return ErrorCode.REDIS_TIMEOUT;
        }
        if (throwable instanceof PluginException pluginException) {
            return pluginException.code();
        }
        return ErrorCode.REDIS_UNAVAILABLE;
    }
}
