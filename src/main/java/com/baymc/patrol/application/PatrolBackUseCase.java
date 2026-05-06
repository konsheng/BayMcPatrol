package com.baymc.patrol.application;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolAction;
import com.baymc.patrol.domain.PatrolHistoryEntry;
import com.baymc.patrol.domain.PatrolSession;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.domain.PendingPatrol;
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
import com.baymc.patrol.velocity.VelocityConnectService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 巡查历史回退用例
 */
public final class PatrolBackUseCase {
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final ErrorService errorService;
    private final SchedulerAdapter scheduler;
    private final RepositoryProvider repositoryProvider;
    private final PendingPatrolRepository pendingRepository;
    private final TeleportService teleportService;
    private final VelocityConnectService velocityConnectService;

    public PatrolBackUseCase(
            ConfigManager configManager,
            LanguageService languageService,
            ErrorService errorService,
            SchedulerAdapter scheduler,
            RepositoryProvider repositoryProvider,
            PendingPatrolRepository pendingRepository,
            TeleportService teleportService,
            VelocityConnectService velocityConnectService
    ) {
        this.configManager = configManager;
        this.languageService = languageService;
        this.errorService = errorService;
        this.scheduler = scheduler;
        this.repositoryProvider = repositoryProvider;
        this.pendingRepository = pendingRepository;
        this.teleportService = teleportService;
        this.velocityConnectService = velocityConnectService;
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
            return moveCursorAndTeleport(staffUuid, staffName, requestId, repositories, keepActive);
        }).whenComplete((ignored, throwable) -> {
            if (throwable != null && activeAcquired.get() && !keepActive.get()) {
                repositories.active().release(staffUuid);
            }
        });
    }

    private CompletableFuture<Void> moveCursorAndTeleport(
            UUID staffUuid,
            String staffName,
            UUID requestId,
            PatrolRepositories repositories,
            AtomicBoolean keepActive
    ) {
        return repositories.session().find(staffUuid).thenCompose(session -> {
            Optional<PatrolHistoryEntry> previous = session.moveBack();
            if (previous.isEmpty()) {
                return send(staffUuid, "messages.no-history").thenCompose(ignored -> repositories.active().release(staffUuid));
            }
            PatrolHistoryEntry entry = previous.get();
            return repositories.session().save(staffUuid, session)
                    .thenCompose(ignored -> repositories.online().findOnlinePlayer(entry.uuid()))
                    .thenCompose(current -> {
                        if (current.isEmpty()) {
                            return send(staffUuid, "messages.target-offline").thenCompose(ignored -> repositories.active().release(staffUuid));
                        }
                        return handleBackTarget(staffUuid, staffName, requestId, repositories, entry, current.get(), keepActive);
                    });
        });
    }

    private CompletableFuture<Void> handleBackTarget(
            UUID staffUuid,
            String staffName,
            UUID requestId,
            PatrolRepositories repositories,
            PatrolHistoryEntry historyEntry,
            PatrolTarget currentTarget,
            AtomicBoolean keepActive
    ) {
        boolean sameServer = currentTarget.serverId().equals(configManager.settings().server().id());
        if (!repositories.redisMode() || sameServer) {
            return teleportService.teleportToTarget(staffUuid, currentTarget.uuid()).thenCompose(success -> {
                if (!success) {
                    return send(staffUuid, "messages.teleport-failed").thenCompose(ignored -> repositories.active().release(staffUuid));
                }
                return repositories.active().release(staffUuid)
                        .thenCompose(ignored -> sendBackSuccess(staffUuid, currentTarget));
            });
        }

        PendingPatrol pendingPatrol = new PendingPatrol(
                requestId,
                PatrolAction.BACK,
                staffUuid,
                staffName,
                historyEntry.uuid(),
                historyEntry.name(),
                currentTarget.serverId(),
                currentTarget.serverAlias(),
                System.currentTimeMillis(),
                0,
                null
        );
        return pendingRepository.save(pendingPatrol)
                .thenCompose(ignored -> send(staffUuid, "messages.switching-server",
                        MessagePlaceholder.unparsed("server_alias", currentTarget.serverAlias())))
                .thenCompose(ignored -> velocityConnectService.connect(staffUuid, currentTarget.serverId()))
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

    private CompletableFuture<Void> sendBackSuccess(UUID staffUuid, PatrolTarget target) {
        return send(staffUuid, "messages.patrol-back",
                MessagePlaceholder.unparsed("target", target.name()),
                MessagePlaceholder.unparsed("server_alias", target.serverAlias()));
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
