package com.baymc.patrol.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 管理员巡查会话
 *
 * seen, history, cursor 只表达巡查状态, 不包含任何 Bukkit 对象
 */
public final class PatrolSession {
    private final Set<UUID> seen;
    private final List<PatrolHistoryEntry> history;
    private int cursor;

    public PatrolSession(Set<UUID> seen, List<PatrolHistoryEntry> history, int cursor) {
        this.seen = new HashSet<>(seen);
        this.history = new ArrayList<>(history);
        this.cursor = cursor;
    }

    public static PatrolSession empty() {
        return new PatrolSession(Set.of(), List.of(), -1);
    }

    public Set<UUID> seen() {
        return new HashSet<>(seen);
    }

    public List<PatrolHistoryEntry> history() {
        return new ArrayList<>(history);
    }

    public int cursor() {
        return cursor;
    }

    public void clearSeen() {
        seen.clear();
    }

    public void markSeen(UUID targetUuid) {
        seen.add(targetUuid);
    }

    public void appendNext(PatrolHistoryEntry entry) {
        while (history.size() > cursor + 1) {
            history.remove(history.size() - 1);
        }
        history.add(entry);
        cursor = history.size() - 1;
    }

    public Optional<PatrolHistoryEntry> moveBack() {
        if (history.isEmpty() || cursor <= 0) {
            return Optional.empty();
        }
        cursor--;
        return Optional.of(history.get(cursor));
    }
}
