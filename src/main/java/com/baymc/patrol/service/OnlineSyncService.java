package com.baymc.patrol.service;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.redis.RedisManager;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 在线玩家同步服务
 */
public final class OnlineSyncService {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final RedisManager redisManager;
    private final SchedulerAdapter scheduler;
    private final OnlinePlayerRepository redisOnlineRepository;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    public OnlineSyncService(
            JavaPlugin plugin,
            ConfigManager configManager,
            LanguageService languageService,
            RedisManager redisManager,
            SchedulerAdapter scheduler,
            OnlinePlayerRepository redisOnlineRepository
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageService = languageService;
        this.redisManager = redisManager;
        this.scheduler = scheduler;
        this.redisOnlineRepository = redisOnlineRepository;
    }

    public synchronized void start() {
        stop();
        if (!configManager.settings().redis().enabled()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "BayMcPatrol-OnlineSync");
            thread.setDaemon(true);
            return thread;
        });
        long period = Math.max(1, configManager.settings().sync().onlineRefreshSeconds());
        task = executor.scheduleAtFixedRate(this::refresh, 0L, period, TimeUnit.SECONDS);
        plugin.getServer().getConsoleSender().sendMessage(languageService.component("console.sync-started"));
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            plugin.getServer().getConsoleSender().sendMessage(languageService.component("console.sync-stopped"));
        }
    }

    public void refresh() {
        if (!configManager.settings().redis().enabled() || !redisManager.isEnabled()) {
            return;
        }
        scheduler.snapshotOnlinePlayers(
                configManager.settings().server().id(),
                configManager.settings().server().alias()
        ).thenCompose(redisOnlineRepository::publishLocalOnline).exceptionally(throwable -> null);
    }
}
