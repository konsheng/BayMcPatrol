package com.baymc.patrol.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 进行中巡查仓库
 */
public interface ActivePatrolRepository {
    CompletableFuture<Boolean> tryAcquire(UUID staffUuid, UUID requestId);

    CompletableFuture<Void> release(UUID staffUuid);
}
