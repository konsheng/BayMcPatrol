package com.baymc.patrol.local;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.OnlinePlayerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 本地在线玩家仓库
 */
public final class LocalOnlinePlayerRepository implements OnlinePlayerRepository {
    private final ConfigManager configManager;
    private final SchedulerAdapter scheduler;

    public LocalOnlinePlayerRepository(ConfigManager configManager, SchedulerAdapter scheduler) {
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Void> publishLocalOnline(List<PatrolTarget> players) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<PatrolTarget>> findAllOnline() {
        return scheduler.snapshotOnlinePlayers(
                configManager.settings().server().id(),
                configManager.settings().server().alias()
        );
    }

    @Override
    public CompletableFuture<Optional<PatrolTarget>> findOnlinePlayer(UUID playerUuid) {
        return findAllOnline().thenApply(players -> players.stream()
                .filter(player -> player.uuid().equals(playerUuid))
                .findFirst());
    }
}
