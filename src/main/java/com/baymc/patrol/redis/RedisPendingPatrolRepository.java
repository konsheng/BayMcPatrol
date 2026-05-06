package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PendingPatrol;
import com.baymc.patrol.service.PendingPatrolRepository;
import com.baymc.patrol.util.JsonCodec;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 跨服等待任务仓库
 */
public final class RedisPendingPatrolRepository implements PendingPatrolRepository {
    private final ConfigManager configManager;
    private final RedisManager redisManager;
    private final RedisKey redisKey;
    private final JsonCodec jsonCodec;

    public RedisPendingPatrolRepository(ConfigManager configManager, RedisManager redisManager, RedisKey redisKey, JsonCodec jsonCodec) {
        this.configManager = configManager;
        this.redisManager = redisManager;
        this.redisKey = redisKey;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public CompletableFuture<Void> save(PendingPatrol pendingPatrol) {
        return redisManager.execute(commands -> {
            commands.setex(
                    redisKey.pending(pendingPatrol.staffUuid()),
                    configManager.settings().patrol().pendingTimeout().toSeconds(),
                    jsonCodec.toJson(pendingPatrol)
            );
            return null;
        });
    }

    @Override
    public CompletableFuture<Optional<PendingPatrol>> find(UUID staffUuid) {
        return redisManager.execute(commands -> {
            String json = commands.get(redisKey.pending(staffUuid));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(jsonCodec.fromJson(json, PendingPatrol.class));
        });
    }

    @Override
    public CompletableFuture<Void> delete(UUID staffUuid) {
        return redisManager.execute(commands -> {
            commands.del(redisKey.pending(staffUuid));
            return null;
        });
    }
}
