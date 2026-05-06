package com.baymc.patrol.listener;

import com.baymc.patrol.service.OnlineSyncService;
import com.baymc.patrol.service.PendingTeleportService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家加入监听器
 */
public final class PlayerJoinListener implements Listener {
    private final OnlineSyncService onlineSyncService;
    private final PendingTeleportService pendingTeleportService;

    public PlayerJoinListener(OnlineSyncService onlineSyncService, PendingTeleportService pendingTeleportService) {
        this.onlineSyncService = onlineSyncService;
        this.pendingTeleportService = pendingTeleportService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        onlineSyncService.refresh();
        pendingTeleportService.handleJoin(event.getPlayer().getUniqueId());
    }
}
