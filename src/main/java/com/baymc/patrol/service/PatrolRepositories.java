package com.baymc.patrol.service;

/**
 * 巡查仓库组合
 *
 * @param online 在线玩家仓库
 * @param session 会话仓库
 * @param active active 仓库
 * @param lastPatrol 上次巡查仓库
 * @param redisMode 是否 Redis 模式
 */
public record PatrolRepositories(
        OnlinePlayerRepository online,
        PatrolSessionRepository session,
        ActivePatrolRepository active,
        LastPatrolRepository lastPatrol,
        boolean redisMode
) {
}
