package com.baymc.patrol.local;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolSession;
import com.baymc.patrol.service.PatrolSessionRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地内存巡查会话仓库
 */
public final class LocalPatrolSessionRepository implements PatrolSessionRepository {
    private final ConfigManager configManager;
    private final Map<UUID, Entry> sessions = new ConcurrentHashMap<>();

    public LocalPatrolSessionRepository(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public CompletableFuture<PatrolSession> find(UUID staffUuid) {
        Entry entry = sessions.get(staffUuid);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) {
            sessions.remove(staffUuid);
            return CompletableFuture.completedFuture(PatrolSession.empty());
        }
        return CompletableFuture.completedFuture(new PatrolSession(entry.session.seen(), entry.session.history(), entry.session.cursor()));
    }

    @Override
    public CompletableFuture<Void> save(UUID staffUuid, PatrolSession session) {
        long expiresAt = System.currentTimeMillis() + configManager.settings().patrol().sessionExpire().toMillis();
        sessions.put(staffUuid, new Entry(new PatrolSession(session.seen(), session.history(), session.cursor()), expiresAt));
        return CompletableFuture.completedFuture(null);
    }

    private record Entry(PatrolSession session, long expiresAt) {
    }
}
