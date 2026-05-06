package com.baymc.patrol.velocity;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.scheduler.SchedulerAdapter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity Connect 插件消息服务
 */
public final class VelocityConnectService {
    private final ConfigManager configManager;
    private final SchedulerAdapter scheduler;

    public VelocityConnectService(ConfigManager configManager, SchedulerAdapter scheduler) {
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Boolean> connect(UUID playerUuid, String serverId) {
        if (!configManager.settings().velocity().enabled()) {
            return CompletableFuture.completedFuture(false);
        }
        byte[] data;
        try {
            data = buildConnectMessage(serverId);
        } catch (IOException exception) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
        }
        return scheduler.sendPluginMessage(playerUuid, configManager.settings().velocity().channel(), data);
    }

    private byte[] buildConnectMessage(String serverId) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream)) {
            outputStream.writeUTF("Connect");
            outputStream.writeUTF(serverId);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
