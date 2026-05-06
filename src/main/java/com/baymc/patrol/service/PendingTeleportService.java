package com.baymc.patrol.service;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolAction;
import com.baymc.patrol.domain.PatrolSession;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.domain.PendingPatrol;
import com.baymc.patrol.error.ErrorCode;
import com.baymc.patrol.error.ErrorReport;
import com.baymc.patrol.error.ErrorService;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.redis.RedisManager;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.util.DurationFormatter;
import com.baymc.patrol.velocity.VelocityConnectService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 跨服 pending 传送服务
 */
public final class PendingTeleportService {
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final ErrorService errorService;
    private final SchedulerAdapter scheduler;
    private final RedisManager redisManager;
    private final OnlinePlayerRepository localOnlineRepository;
    private final OnlinePlayerRepository redisOnlineRepository;
    private final PatrolSessionRepository redisSessionRepository;
    private final ActivePatrolRepository redisActiveRepository;
    private final PendingPatrolRepository pendingRepository;
    private final LastPatrolRepository redisLastPatrolRepository;
    private final TeleportService teleportService;
    private final VelocityConnectService velocityConnectService;
    private final DurationFormatter durationFormatter;

    public PendingTeleportService(
            ConfigManager configManager,
            LanguageService languageService,
            ErrorService errorService,
            SchedulerAdapter scheduler,
            RedisManager redisManager,
            OnlinePlayerRepository localOnlineRepository,
            OnlinePlayerRepository redisOnlineRepository,
            PatrolSessionRepository redisSessionRepository,
            ActivePatrolRepository redisActiveRepository,
            PendingPatrolRepository pendingRepository,
            LastPatrolRepository redisLastPatrolRepository,
            TeleportService teleportService,
            VelocityConnectService velocityConnectService,
            DurationFormatter durationFormatter
    ) {
        this.configManager = configManager;
        this.languageService = languageService;
        this.errorService = errorService;
        this.scheduler = scheduler;
        this.redisManager = redisManager;
        this.localOnlineRepository = localOnlineRepository;
        this.redisOnlineRepository = redisOnlineRepository;
        this.redisSessionRepository = redisSessionRepository;
        this.redisActiveRepository = redisActiveRepository;
        this.pendingRepository = pendingRepository;
        this.redisLastPatrolRepository = redisLastPatrolRepository;
        this.teleportService = teleportService;
        this.velocityConnectService = velocityConnectService;
        this.durationFormatter = durationFormatter;
    }

    public void handleJoin(UUID staffUuid) {
        if (!redisManager.isAvailable()) {
            return;
        }
        scheduler.runLaterGlobal(
                () -> consume(staffUuid).exceptionally(throwable -> {
                    sendInternalError(staffUuid, throwable);
                    return null;
                }),
                configManager.settings().teleport().delayAfterJoinTicks()
        );
    }

    private CompletableFuture<Void> consume(UUID staffUuid) {
        if (!redisManager.isAvailable()) {
            return CompletableFuture.completedFuture(null);
        }
        return pendingRepository.find(staffUuid).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            PendingPatrol pendingPatrol = optional.get();
            if (isExpired(pendingPatrol)) {
                return cleanupWithMessage(pendingPatrol, "messages.pending-expired");
            }
            return localOnlineRepository.findOnlinePlayer(pendingPatrol.targetUuid())
                    .thenCompose(localTarget -> {
                        if (localTarget.isPresent()) {
                            return teleportAndComplete(pendingPatrol, localTarget.get());
                        }
                        return redirectOrFail(pendingPatrol);
                    });
        });
    }

    private CompletableFuture<Void> teleportAndComplete(PendingPatrol pendingPatrol, PatrolTarget localTarget) {
        return teleportService.teleportToTarget(pendingPatrol.staffUuid(), pendingPatrol.targetUuid()).thenCompose(success -> {
            if (!success) {
                return cleanupWithMessage(pendingPatrol, "messages.teleport-failed");
            }
            if (pendingPatrol.action() == PatrolAction.BACK) {
                return pendingRepository.delete(pendingPatrol.staffUuid())
                        .thenCompose(ignored -> redisActiveRepository.release(pendingPatrol.staffUuid()))
                        .thenCompose(ignored -> send(pendingPatrol.staffUuid(), "messages.patrol-back",
                                MessagePlaceholder.unparsed("target", localTarget.name()),
                                MessagePlaceholder.unparsed("server_alias", localTarget.serverAlias())));
            }
            long now = System.currentTimeMillis();
            return redisSessionRepository.find(pendingPatrol.staffUuid())
                    .thenCompose(session -> saveNextSuccess(pendingPatrol, localTarget, session, now))
                    .thenCompose(ignored -> pendingRepository.delete(pendingPatrol.staffUuid()))
                    .thenCompose(ignored -> redisActiveRepository.release(pendingPatrol.staffUuid()))
                    .thenCompose(ignored -> send(pendingPatrol.staffUuid(), "messages.patrol-next",
                            MessagePlaceholder.unparsed("target", localTarget.name()),
                            MessagePlaceholder.unparsed("server_alias", localTarget.serverAlias()),
                            MessagePlaceholder.unparsed("last_patrol", durationFormatter.formatPrevious(pendingPatrol.previousLastPatrolAt(), now))));
        });
    }

    private CompletableFuture<Void> saveNextSuccess(PendingPatrol pendingPatrol, PatrolTarget localTarget, PatrolSession session, long now) {
        session.markSeen(localTarget.uuid());
        session.appendNext(localTarget.toHistoryEntry());
        return redisSessionRepository.save(pendingPatrol.staffUuid(), session)
                .thenCompose(ignored -> redisLastPatrolRepository.save(pendingPatrol.staffUuid(), localTarget.uuid(), now));
    }

    private CompletableFuture<Void> redirectOrFail(PendingPatrol pendingPatrol) {
        return redisOnlineRepository.findOnlinePlayer(pendingPatrol.targetUuid()).thenCompose(current -> {
            if (current.isPresent()
                    && pendingPatrol.redirects() == 0
                    && !current.get().serverId().equals(configManager.settings().server().id())) {
                PatrolTarget target = current.get();
                PendingPatrol redirected = pendingPatrol.redirectTo(target.serverId(), target.serverAlias());
                return pendingRepository.save(redirected)
                        .thenCompose(ignored -> send(pendingPatrol.staffUuid(), "messages.switching-server",
                                MessagePlaceholder.unparsed("server_alias", target.serverAlias())))
                        .thenCompose(ignored -> velocityConnectService.connect(pendingPatrol.staffUuid(), target.serverId()))
                        .thenCompose(sent -> {
                            if (sent) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return cleanupWithMessage(pendingPatrol, "messages.server-switch-failed");
                        });
            }
            return cleanupWithMessage(pendingPatrol, "messages.target-offline");
        });
    }

    private CompletableFuture<Void> cleanupWithMessage(PendingPatrol pendingPatrol, String key) {
        return pendingRepository.delete(pendingPatrol.staffUuid())
                .thenCompose(ignored -> redisActiveRepository.release(pendingPatrol.staffUuid()))
                .thenCompose(ignored -> send(pendingPatrol.staffUuid(), key));
    }

    private boolean isExpired(PendingPatrol pendingPatrol) {
        long age = System.currentTimeMillis() - pendingPatrol.createdAt();
        return age > configManager.settings().patrol().pendingTimeout().toMillis();
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
