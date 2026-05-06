package com.baymc.patrol.config;

import java.time.Duration;

/**
 * 插件配置快照
 */
public record PluginSettings(
        ServerSettings server,
        RedisSettings redis,
        VelocitySettings velocity,
        PatrolSettings patrol,
        TeleportSettings teleport,
        SyncSettings sync,
        LanguageSettings language,
        RuntimeSettings runtime
) {
    public record ServerSettings(String id, String alias) {
    }

    public record RedisSettings(
            boolean enabled,
            String keyPrefix,
            String host,
            int port,
            String password,
            int database,
            boolean ssl,
            Duration connectTimeout,
            Duration commandTimeout,
            boolean reconnectEnabled,
            Duration reconnectDelay
    ) {
    }

    public record VelocitySettings(boolean enabled, String channel) {
    }

    public record PatrolSettings(
            boolean sameServerFirst,
            Duration sessionExpire,
            Duration pendingTimeout,
            Duration activeTimeout
    ) {
    }

    public record TeleportSettings(int delayAfterJoinTicks) {
    }

    public record SyncSettings(
            int onlineRefreshSeconds,
            Duration onlineTtl,
            Duration heartbeatTtl
    ) {
    }

    public record LanguageSettings(String defaultLanguage) {
    }

    public record RuntimeSettings(ErrorSettings error) {
    }

    public record ErrorSettings(
            boolean debug,
            boolean printStacktraceToConsole,
            boolean showTraceIdToPlayer,
            int keepLastErrors
    ) {
    }
}
