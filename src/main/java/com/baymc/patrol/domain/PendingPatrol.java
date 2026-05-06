package com.baymc.patrol.domain;

import java.util.UUID;

/**
 * 跨服巡查等待任务
 *
 * @param requestId 请求 ID
 * @param action 巡查动作
 * @param staffUuid 管理员 UUID
 * @param staffName 管理员名称
 * @param targetUuid 目标 UUID
 * @param targetName 目标名称
 * @param targetServer 目标服务器 ID
 * @param targetServerAlias 目标服务器显示名
 * @param createdAt 创建时间
 * @param redirects 已追踪次数
 * @param previousLastPatrolAt 旧的上次巡查时间
 */
public record PendingPatrol(
        UUID requestId,
        PatrolAction action,
        UUID staffUuid,
        String staffName,
        UUID targetUuid,
        String targetName,
        String targetServer,
        String targetServerAlias,
        long createdAt,
        int redirects,
        Long previousLastPatrolAt
) {
    public PendingPatrol redirectTo(String serverId, String serverAlias) {
        return new PendingPatrol(
                requestId,
                action,
                staffUuid,
                staffName,
                targetUuid,
                targetName,
                serverId,
                serverAlias,
                createdAt,
                redirects + 1,
                previousLastPatrolAt
        );
    }

    public PatrolHistoryEntry targetHistoryEntry() {
        return new PatrolHistoryEntry(targetUuid, targetName, targetServer, targetServerAlias);
    }
}
