package com.baymc.patrol.redis;

import com.baymc.patrol.config.ConfigManager;

import java.util.UUID;

/**
 * Redis key 生成器
 */
public final class RedisKey {
    private final ConfigManager configManager;

    public RedisKey(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public String servers() {
        return prefix() + ":servers";
    }

    public String heartbeat(String serverId) {
        return prefix() + ":heartbeat:" + serverId;
    }

    public String serverOnline(String serverId) {
        return prefix() + ":server:online:" + serverId;
    }

    public String serverAlias(String serverId) {
        return prefix() + ":server:alias:" + serverId;
    }

    public String playerServer(UUID uuid) {
        return prefix() + ":player:server:" + uuid;
    }

    public String playerName(UUID uuid) {
        return prefix() + ":player:name:" + uuid;
    }

    public String playerBypass(UUID uuid) {
        return prefix() + ":player:bypass:" + uuid;
    }

    public String pending(UUID staffUuid) {
        return prefix() + ":pending:" + staffUuid;
    }

    public String active(UUID staffUuid) {
        return prefix() + ":active:" + staffUuid;
    }

    public String seen(UUID staffUuid) {
        return prefix() + ":seen:" + staffUuid;
    }

    public String history(UUID staffUuid) {
        return prefix() + ":history:" + staffUuid;
    }

    public String cursor(UUID staffUuid) {
        return prefix() + ":cursor:" + staffUuid;
    }

    public String last(UUID staffUuid, UUID targetUuid) {
        return prefix() + ":last:" + staffUuid + ":" + targetUuid;
    }

    private String prefix() {
        return configManager.settings().redis().keyPrefix();
    }
}
