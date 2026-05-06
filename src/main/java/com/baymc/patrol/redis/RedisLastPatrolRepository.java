package com.baymc.patrol.redis;

import com.baymc.patrol.service.LastPatrolRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 上次巡查时间仓库
 */
public final class RedisLastPatrolRepository implements LastPatrolRepository {
    private final RedisManager redisManager;
    private final RedisKey redisKey;

    public RedisLastPatrolRepository(RedisManager redisManager, RedisKey redisKey) {
        this.redisManager = redisManager;
        this.redisKey = redisKey;
    }

    @Override
    public CompletableFuture<Optional<Long>> find(UUID staffUuid, UUID targetUuid) {
        return redisManager.execute(commands -> {
            String value = commands.get(redisKey.last(staffUuid, targetUuid));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(value));
        });
    }

    @Override
    public CompletableFuture<Void> save(UUID staffUuid, UUID targetUuid, long timestampMillis) {
        return redisManager.execute(commands -> {
            commands.set(redisKey.last(staffUuid, targetUuid), Long.toString(timestampMillis));
            return null;
        });
    }
}
