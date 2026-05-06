package com.baymc.patrol.domain;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机目标选择器
 */
public final class TargetSelector {
    public Optional<PatrolTarget> select(List<PatrolTarget> candidates, String localServerId, boolean sameServerFirst) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        List<PatrolTarget> pool = candidates;
        if (sameServerFirst) {
            List<PatrolTarget> localCandidates = candidates.stream()
                    .filter(target -> target.serverId().equals(localServerId))
                    .toList();
            if (!localCandidates.isEmpty()) {
                pool = localCandidates;
            }
        }
        return Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
    }
}
