package com.baymc.patrol.application;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolAction;
import com.baymc.patrol.domain.PatrolSession;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.domain.PendingPatrol;
import com.baymc.patrol.domain.TargetSelector;
import com.baymc.patrol.error.ErrorCode;
import com.baymc.patrol.error.ErrorReport;
import com.baymc.patrol.error.ErrorService;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.PatrolRepositories;
import com.baymc.patrol.service.PendingPatrolRepository;
import com.baymc.patrol.service.RepositoryProvider;
import com.baymc.patrol.service.TeleportService;
import com.baymc.patrol.util.DurationFormatter;
import com.baymc.patrol.velocity.VelocityConnectService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 随机巡查下一个玩家用例
 */
public final class PatrolNextUseCase {
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final ErrorService errorService;
    private final SchedulerAdapter scheduler;
    private final RepositoryProvider repositoryProvider;
    private final PendingPatrolRepository pendingRepository;
    private final TeleportService teleportService;
    private final VelocityConnectService velocityConnectService;
    private final TargetSelector targetSelector;
    private final DurationFormatter durationFormatter;

    public PatrolNextUseCase(
            ConfigManager configManager,
            LanguageService languageService,
            ErrorService errorService,
            SchedulerAdapter scheduler,
            RepositoryProvider repositoryProvider,
            PendingPatrolRepository pendingRepository,
            TeleportService teleportService,
            VelocityConnectService velocityConnectService,
            TargetSelector targetSelector,
            DurationFormatter durationFormatter
    ) {
        this.configManager = configManager;
        this.languageService = languageService;
        this.errorService = errorService;
        this.scheduler = scheduler;
        this.repositoryProvider = repositoryProvider;
        this.pendingRepository = pendingRepository;
        this.teleportService = teleportService;
        this.velocityConnectService = velocityConnectService;
        this.targetSelector = targetSelector;
        this.durationFormatter = durationFormatter;
    }

    public void execute(UUID staffUuid, String staffName) {
        PatrolRepositories repositories = repositoryProvider.current();
        run(staffUuid, staffName, repositories).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                return;
            }
            if (repositories.redisMode()) {
                run(staffUuid, staffName, repositoryProvider.local()).exceptionally(localError -> {
                    sendInternalError(staffUuid, localError);
                    return null;
                });
                return;
            }
            sendInternalError(staffUuid, throwable);
        });
    }

    private CompletableFuture<Void> run(UUID staffUuid, String staffName, PatrolRepositories repositories) {
        UUID requestId = UUID.randomUUID();
        AtomicBoolean activeAcquired = new AtomicBoolean(false);
        AtomicBoolean keepActive = new AtomicBoolean(false);
        return repositories.active().tryAcquire(staffUuid, requestId).thenCompose(acquired -> {
            if (!acquired) {
                return send(staffUuid, "messages.patrol-active");
            }
            activeAcquired.set(true);
            return selectAndTeleport(staffUuid, staffName, requestId, repositories, keepActive);
        }).whenComplete((ignored, throwable) -> {
            if (throwable != null && activeAcquired.get() && !keepActive.get()) {
                repositories.active().release(staffUuid);
            }
        });
    }

    private CompletableFuture<Void> selectAndTeleport(
            UUID staffUuid,
            String staffName,
            UUID requestId,
            PatrolRepositories repositories,
            AtomicBoolean keepActive
    ) {
        return repositories.session().find(staffUuid).thenCompose(session -> repositories.online().findAllOnline().thenCompose(onlinePlayers -> {
            List<PatrolTarget> candidates = filterCandidates(onlinePlayers, staffUuid, session.seen());
            if (candidates.isEmpty()) {
                session.clearSeen();
                candidates = filterCandidates(onlinePlayers, staffUuid, session.seen());
            }
            if (candidates.isEmpty()) {
                return repositories.session().save(staffUuid, session)
                        .thenCompose(ignored -> send(staffUuid, "messages.no-target"))
                        .thenCompose(ignored -> repositories.active().release(staffUuid));
            }

            Optional<PatrolTarget> selected = targetSelector.select(
                    candidates,
                    configManager.settings().server().id(),
                    configManager.settings().patrol().sameServerFirst()
            );
            if (selected.isEmpty()) {
                return send(staffUuid, "messages.no-target").thenCompose(ignored -> repositories.active().release(staffUuid));
            }
            PatrolTarget target = selected.get();
            return repositories.lastPatrol().find(staffUuid, target.uuid())
                    .thenCompose(previous -> handleTarget(staffUuid, staffName, requestId, repositories, session, target, previous.orElse(null), keepActive));
        }));
    }

    private CompletableFuture<Void> handleTarget(
            UUID staffUuid,
            String staffName,
            UUID requestId,
            PatrolRepositories repositories,
            PatrolSession session,
            PatrolTarget target,
            Long previousLastPatrol,
            AtomicBoolean keepActive
    ) {
        boolean sameServer = target.serverId().equals(configManager.settings().server().id());
        if (!repositories.redisMode() || sameServer) {
            return teleportLocalNext(staffUuid, repositories, session, target, previousLastPatrol);
        }

        PendingPatrol pendingPatrol = new PendingPatrol(
                requestId,
                PatrolAction.NEXT,
                staffUuid,
                staffName,
                target.uuid(),
                target.name(),
                target.serverId(),
                target.serverAlias(),
                System.currentTimeMillis(),
                0,
                previousLastPatrol
        );
        return pendingRepository.save(pendingPatrol)
                .thenCompose(ignored -> send(staffUuid, "messages.switching-server",
                        MessagePlaceholder.unparsed("server_alias", target.serverAlias())))
                .thenCompose(ignored -> velocityConnectService.connect(staffUuid, target.serverId()))
                .thenCompose(sent -> {
                    if (!sent) {
                        return pendingRepository.delete(staffUuid)
                                .thenCompose(ignored -> send(staffUuid, "messages.server-switch-failed"))
                                .thenCompose(ignored -> repositories.active().release(staffUuid));
                    }
                    keepActive.set(true);
                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Void> teleportLocalNext(
            UUID staffUuid,
            PatrolRepositories repositories,
            PatrolSession session,
            PatrolTarget target,
            Long previousLastPatrol
    ) {
        return teleportService.teleportToTarget(staffUuid, target.uuid()).thenCompose(success -> {
            if (!success) {
                return send(staffUuid, "messages.teleport-failed").thenCompose(ignored -> repositories.active().release(staffUuid));
            }
            long now = System.currentTimeMillis();
            session.markSeen(target.uuid());
            session.appendNext(target.toHistoryEntry());
            return repositories.session().save(staffUuid, session)
                    .thenCompose(ignored -> repositories.lastPatrol().save(staffUuid, target.uuid(), now))
                    .thenCompose(ignored -> repositories.active().release(staffUuid))
                    .thenCompose(ignored -> sendNextSuccess(staffUuid, target, previousLastPatrol, now));
        });
    }

    private List<PatrolTarget> filterCandidates(List<PatrolTarget> onlinePlayers, UUID staffUuid, Set<UUID> seen) {
        return onlinePlayers.stream()
                .filter(player -> !player.uuid().equals(staffUuid))
                .filter(player -> !player.bypass())
                .filter(player -> !seen.contains(player.uuid()))
                .toList();
    }

    private CompletableFuture<Void> sendNextSuccess(UUID staffUuid, PatrolTarget target, Long previousLastPatrol, long now) {
        return send(staffUuid, "messages.patrol-next",
                MessagePlaceholder.unparsed("target", target.name()),
                MessagePlaceholder.unparsed("server_alias", target.serverAlias()),
                MessagePlaceholder.unparsed("last_patrol", durationFormatter.formatPrevious(previousLastPatrol, now)));
    }

    private CompletableFuture<Void> send(UUID staffUuid, String key, MessagePlaceholder... placeholders) {
        return scheduler.sendMessage(staffUuid, languageService.component(key, placeholders));
    }

    private void sendInternalError(UUID staffUuid, Throwable throwable) {
        ErrorReport report = errorService.report(ErrorCode.INTERNAL_ERROR, throwable);
        scheduler.sendMessage(staffUuid, languageService.component(
                "error.internal",
                MessagePlaceholder.unparsed("trace_id", report.traceId())
        ));
    }
}
