package com.baymc.patrol.local;

import com.baymc.patrol.service.LastPatrolRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存上次巡查时间仓库
 */
public final class LocalLastPatrolRepository implements LastPatrolRepository {
    private final Map<String, Long> values = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<Long>> find(UUID staffUuid, UUID targetUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(values.get(key(staffUuid, targetUuid))));
    }

    @Override
    public CompletableFuture<Void> save(UUID staffUuid, UUID targetUuid, long timestampMillis) {
        values.put(key(staffUuid, targetUuid), timestampMillis);
        return CompletableFuture.completedFuture(null);
    }

    private String key(UUID staffUuid, UUID targetUuid) {
        return staffUuid + ":" + targetUuid;
    }
}
