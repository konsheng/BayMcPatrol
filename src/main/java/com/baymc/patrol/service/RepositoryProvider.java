package com.baymc.patrol.service;

/**
 * 根据运行状态选择仓库
 */
public final class RepositoryProvider {
    private final ServerModeService serverModeService;
    private final PatrolRepositories redisRepositories;
    private final PatrolRepositories localRepositories;

    public RepositoryProvider(
            ServerModeService serverModeService,
            PatrolRepositories redisRepositories,
            PatrolRepositories localRepositories
    ) {
        this.serverModeService = serverModeService;
        this.redisRepositories = redisRepositories;
        this.localRepositories = localRepositories;
    }

    public PatrolRepositories current() {
        if (serverModeService.isCrossServerMode()) {
            return redisRepositories;
        }
        return localRepositories;
    }

    public PatrolRepositories local() {
        return localRepositories;
    }

    public PatrolRepositories redis() {
        return redisRepositories;
    }
}
