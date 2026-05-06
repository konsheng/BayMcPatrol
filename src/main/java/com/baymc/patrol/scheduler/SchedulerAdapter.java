package com.baymc.patrol.scheduler;

import com.baymc.patrol.domain.PatrolTarget;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 调度器适配接口
 *
 * 业务层通过该接口访问玩家相关操作, 避免直接依赖 Paper 或 Folia 调度差异
 */
public interface SchedulerAdapter {
    void runAsync(Runnable task);

    void runGlobal(Runnable task);

    void runLaterGlobal(Runnable task, long delayTicks);

    void runForPlayer(UUID playerUuid, Consumer<Player> task);

    CompletableFuture<Location> getPlayerLocation(UUID playerUuid);

    CompletableFuture<Boolean> teleportToPlayer(UUID staffUuid, UUID targetUuid);

    CompletableFuture<Void> sendMessage(UUID playerUuid, Component message);

    CompletableFuture<Boolean> sendPluginMessage(UUID playerUuid, String channel, byte[] data);

    CompletableFuture<List<PatrolTarget>> snapshotOnlinePlayers(String serverId, String serverAlias);

    boolean isFolia();

    void shutdown();
}
