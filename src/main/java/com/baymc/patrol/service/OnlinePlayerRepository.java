package com.baymc.patrol.service;

import com.baymc.patrol.domain.PatrolTarget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 在线玩家仓库
 */
public interface OnlinePlayerRepository {
    CompletableFuture<Void> publishLocalOnline(List<PatrolTarget> players);

    CompletableFuture<List<PatrolTarget>> findAllOnline();

    CompletableFuture<Optional<PatrolTarget>> findOnlinePlayer(UUID playerUuid);
}
