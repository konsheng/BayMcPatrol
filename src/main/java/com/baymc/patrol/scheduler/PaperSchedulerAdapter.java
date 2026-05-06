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
 * Paper 调度器适配
 */
public class PaperSchedulerAdapter implements SchedulerAdapter {
    protected final JavaPlugin plugin;

    public PaperSchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runLaterGlobal(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runForPlayer(UUID playerUuid, Consumer<Player> task) {
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                task.accept(player);
            }
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
            future.complete(player.getLocation().clone());
        });
        return future;
    }

    @Override
    public CompletableFuture<Boolean> teleportToPlayer(UUID staffUuid, UUID targetUuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runGlobal(() -> {
            Player staff = Bukkit.getPlayer(staffUuid);
            Player target = Bukkit.getPlayer(targetUuid);
            if (staff == null || target == null || !staff.isOnline() || !target.isOnline()) {
                future.complete(false);
                return;
            }
            Location location = target.getLocation().clone();
            staff.teleportAsync(location).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                    return;
                }
                future.complete(Boolean.TRUE.equals(result));
            });
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> sendMessage(UUID playerUuid, Component message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runGlobal(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
            future.complete(null);
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
            player.sendPluginMessage(plugin, channel, data);
            future.complete(true);
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
        return false;
    }

    @Override
    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
