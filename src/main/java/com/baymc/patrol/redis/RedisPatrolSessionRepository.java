package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolHistoryEntry;
import com.baymc.patrol.domain.PatrolSession;
import com.baymc.patrol.service.PatrolSessionRepository;
import com.baymc.patrol.util.JsonCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Redis 巡查会话仓库
 */
public final class RedisPatrolSessionRepository implements PatrolSessionRepository {
    private final ConfigManager configManager;
    private final RedisManager redisManager;
    private final RedisKey redisKey;
    private final JsonCodec jsonCodec;

    public RedisPatrolSessionRepository(ConfigManager configManager, RedisManager redisManager, RedisKey redisKey, JsonCodec jsonCodec) {
        this.configManager = configManager;
        this.redisManager = redisManager;
        this.redisKey = redisKey;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public CompletableFuture<PatrolSession> find(UUID staffUuid) {
        return redisManager.execute(commands -> {
            Set<UUID> seen = new HashSet<>();
            for (String uuidText : commands.smembers(redisKey.seen(staffUuid))) {
                seen.add(UUID.fromString(uuidText));
            }
            List<PatrolHistoryEntry> history = commands.lrange(redisKey.history(staffUuid), 0L, -1L)
                    .stream()
                    .map(json -> jsonCodec.fromJson(json, PatrolHistoryEntry.class))
                    .toList();
            String cursorText = commands.get(redisKey.cursor(staffUuid));
            int cursor = cursorText == null ? history.size() - 1 : Integer.parseInt(cursorText);
            return new PatrolSession(seen, history, cursor);
        });
    }

    @Override
    public CompletableFuture<Void> save(UUID staffUuid, PatrolSession session) {
        return redisManager.execute(commands -> {
            long ttl = configManager.settings().patrol().sessionExpire().toSeconds();
            commands.del(redisKey.seen(staffUuid), redisKey.history(staffUuid), redisKey.cursor(staffUuid));
            Set<UUID> seen = session.seen();
            if (!seen.isEmpty()) {
                commands.sadd(redisKey.seen(staffUuid), seen.stream().map(UUID::toString).toArray(String[]::new));
                commands.expire(redisKey.seen(staffUuid), ttl);
            }
            List<PatrolHistoryEntry> history = session.history();
            if (!history.isEmpty()) {
                commands.rpush(redisKey.history(staffUuid), history.stream().map(jsonCodec::toJson).toArray(String[]::new));
                commands.expire(redisKey.history(staffUuid), ttl);
            }
            commands.setex(redisKey.cursor(staffUuid), ttl, Integer.toString(session.cursor()));
            return null;
        });
    }
}
