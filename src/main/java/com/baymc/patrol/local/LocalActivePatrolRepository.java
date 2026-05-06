package com.baymc.patrol.local;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.service.ActivePatrolRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存 active 巡查仓库
 */
public final class LocalActivePatrolRepository implements ActivePatrolRepository {
    private final ConfigManager configManager;
    private final Map<UUID, Entry> active = new ConcurrentHashMap<>();

    public LocalActivePatrolRepository(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public synchronized CompletableFuture<Boolean> tryAcquire(UUID staffUuid, UUID requestId) {
        long now = System.currentTimeMillis();
        Entry existing = active.get(staffUuid);
        if (existing != null && existing.expiresAt >= now) {
            return CompletableFuture.completedFuture(false);
        }
        long expiresAt = now + configManager.settings().patrol().activeTimeout().toMillis();
        active.put(staffUuid, new Entry(requestId, expiresAt));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public synchronized CompletableFuture<Void> release(UUID staffUuid) {
        active.remove(staffUuid);
        return CompletableFuture.completedFuture(null);
    }

    private record Entry(UUID requestId, long expiresAt) {
    }
}
