package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.service.ActivePatrolRepository;
import io.lettuce.core.SetArgs;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis active 巡查仓库
 */
public final class RedisActivePatrolRepository implements ActivePatrolRepository {
    private final ConfigManager configManager;
    private final RedisManager redisManager;
    private final RedisKey redisKey;

    public RedisActivePatrolRepository(ConfigManager configManager, RedisManager redisManager, RedisKey redisKey) {
        this.configManager = configManager;
        this.redisManager = redisManager;
        this.redisKey = redisKey;
    }

    @Override
    public CompletableFuture<Boolean> tryAcquire(UUID staffUuid, UUID requestId) {
        return redisManager.execute(commands -> "OK".equals(commands.set(
                redisKey.active(staffUuid),
                requestId.toString(),
                SetArgs.Builder.nx().ex(configManager.settings().patrol().activeTimeout().toSeconds())
        )));
    }

    @Override
    public CompletableFuture<Void> release(UUID staffUuid) {
        return redisManager.execute(commands -> {
            commands.del(redisKey.active(staffUuid));
            return null;
        });
    }
}
