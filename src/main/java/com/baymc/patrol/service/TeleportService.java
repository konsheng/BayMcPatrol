package com.baymc.patrol.service;

import com.baymc.patrol.scheduler.SchedulerAdapter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 传送服务
 */
public final class TeleportService {
    private final SchedulerAdapter scheduler;

    public TeleportService(SchedulerAdapter scheduler) {
        this.scheduler = scheduler;
    }

    public CompletableFuture<Boolean> teleportToTarget(UUID staffUuid, UUID targetUuid) {
        return scheduler.teleportToPlayer(staffUuid, targetUuid);
    }
}
