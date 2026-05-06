package com.baymc.patrol.listener;

import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.OnlineSyncService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家退出监听器
 */
public final class PlayerQuitListener implements Listener {
    private final SchedulerAdapter scheduler;
    private final OnlineSyncService onlineSyncService;

    public PlayerQuitListener(SchedulerAdapter scheduler, OnlineSyncService onlineSyncService) {
        this.scheduler = scheduler;
        this.onlineSyncService = onlineSyncService;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scheduler.runLaterGlobal(onlineSyncService::refresh, 1L);
    }
}
