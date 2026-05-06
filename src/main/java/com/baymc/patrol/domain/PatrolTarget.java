package com.baymc.patrol.domain;

import java.util.UUID;

/**
 * 在线巡查候选目标
 *
 * @param uuid 玩家 UUID
 * @param name 玩家名
 * @param serverId 所在服务器 ID
 * @param serverAlias 所在服务器显示名
 * @param bypass 是否拥有随机巡查绕过权限
 */
public record PatrolTarget(
        UUID uuid,
        String name,
        String serverId,
        String serverAlias,
        boolean bypass
) {
    public PatrolHistoryEntry toHistoryEntry() {
        return new PatrolHistoryEntry(uuid, name, serverId, serverAlias);
    }
}
