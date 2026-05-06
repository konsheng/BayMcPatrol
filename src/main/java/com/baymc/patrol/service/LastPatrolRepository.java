package com.baymc.patrol.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 上次巡查时间仓库
 */
public interface LastPatrolRepository {
    CompletableFuture<Optional<Long>> find(UUID staffUuid, UUID targetUuid);

    CompletableFuture<Void> save(UUID staffUuid, UUID targetUuid, long timestampMillis);
}
