package com.baymc.patrol.scheduler;

import com.baymc.patrol.domain.PatrolTarget;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Folia 调度器适配
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin plugin;

    public FoliaSchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    @Override
    public void runLaterGlobal(Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }

    @Override
    public void runForPlayer(UUID playerUuid, Consumer<Player> task) {
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return;
            }
            player.getScheduler().run(plugin, scheduledTask -> task.accept(player), null);
        });
    }

    @Override
    public CompletableFuture<Location> getPlayerLocation(UUID playerUuid) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                future.complete(null);
                return;
            }
            player.getScheduler().run(plugin, scheduledTask -> future.complete(player.getLocation().clone()), null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> teleportToPlayer(UUID staffUuid, UUID targetUuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getPlayerLocation(targetUuid).whenComplete((location, locationError) -> {
            if (locationError != null) {
                future.completeExceptionally(locationError);
                return;
            }
            if (location == null) {
                future.complete(false);
                return;
            }
            runGlobal(() -> {
                Player staff = Bukkit.getPlayer(staffUuid);
                if (staff == null || !staff.isOnline()) {
                    future.complete(false);
                    return;
                }
                staff.getScheduler().run(plugin, scheduledTask -> staff.teleportAsync(location).whenComplete((result, teleportError) -> {
                    if (teleportError != null) {
                        future.completeExceptionally(teleportError);
                        return;
                    }
                    future.complete(Boolean.TRUE.equals(result));
                }), null);
            });
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> sendMessage(UUID playerUuid, Component message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                future.complete(null);
                return;
            }
            player.getScheduler().run(plugin, scheduledTask -> {
                player.sendMessage(message);
                future.complete(null);
            }, null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> sendPluginMessage(UUID playerUuid, String channel, byte[] data) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                future.complete(false);
                return;
            }
            player.getScheduler().run(plugin, scheduledTask -> {
                player.sendPluginMessage(plugin, channel, data);
                future.complete(true);
            }, null);
        });
        return future;
    }

    @Override
    public CompletableFuture<List<PatrolTarget>> snapshotOnlinePlayers(String serverId, String serverAlias) {
        CompletableFuture<List<PatrolTarget>> future = new CompletableFuture<>();
        runGlobal(() -> {
            List<PatrolTarget> targets = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                targets.add(new PatrolTarget(
                        player.getUniqueId(),
                        player.getName(),
                        serverId,
                        serverAlias,
                        player.hasPermission("baymcpatrol.bypass")
                ));
            }
            future.complete(targets);
        });
        return future;
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    @Override
    public void shutdown() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }
}
