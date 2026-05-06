package com.baymc.patrol.service;

import com.baymc.patrol.domain.PendingPatrol;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 跨服等待任务仓库
 */
public interface PendingPatrolRepository {
    CompletableFuture<Void> save(PendingPatrol pendingPatrol);

    CompletableFuture<Optional<PendingPatrol>> find(UUID staffUuid);

    CompletableFuture<Void> delete(UUID staffUuid);
}
