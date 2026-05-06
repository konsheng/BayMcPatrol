package com.baymc.patrol.service;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.redis.RedisManager;

/**
 * 运行模式服务
 */
public final class ServerModeService {
    private final ConfigManager configManager;
    private final RedisManager redisManager;

    public ServerModeService(ConfigManager configManager, RedisManager redisManager) {
        this.configManager = configManager;
        this.redisManager = redisManager;
    }

    public RunMode currentMode() {
        if (!configManager.settings().redis().enabled()) {
            return RunMode.LOCAL_ONLY;
        }
        if (redisManager.isAvailable()) {
            return RunMode.CROSS_SERVER;
        }
        return RunMode.LOCAL_FALLBACK;
    }

    public boolean isCrossServerMode() {
        return currentMode() == RunMode.CROSS_SERVER;
    }
}
