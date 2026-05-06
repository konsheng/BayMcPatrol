package com.baymc.patrol.domain;

import java.util.UUID;

/**
 * 巡查历史目标快照
 *
 * @param uuid 玩家 UUID
 * @param name 玩家名
 * @param serverId 所在服务器 ID
 * @param serverAlias 所在服务器显示名
 */
public record PatrolHistoryEntry(
        UUID uuid,
        String name,
        String serverId,
        String serverAlias
) {
}
