package com.baymc.patrol.service;

import com.baymc.patrol.domain.PatrolSession;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 巡查会话仓库
 */
public interface PatrolSessionRepository {
    CompletableFuture<PatrolSession> find(UUID staffUuid);

    CompletableFuture<Void> save(UUID staffUuid, PatrolSession session);
}
