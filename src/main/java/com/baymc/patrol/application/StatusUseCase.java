package com.baymc.patrol.application;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.PatrolTarget;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.redis.RedisManager;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.OnlinePlayerRepository;
import com.baymc.patrol.service.RunMode;
import com.baymc.patrol.service.ServerModeService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 插件状态用例
 */
public final class StatusUseCase {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final SchedulerAdapter scheduler;
    private final RedisManager redisManager;
    private final ServerModeService serverModeService;
    private final OnlinePlayerRepository localOnlineRepository;
    private final OnlinePlayerRepository redisOnlineRepository;

    public StatusUseCase(
            JavaPlugin plugin,
            ConfigManager configManager,
            LanguageService languageService,
            SchedulerAdapter scheduler,
            RedisManager redisManager,
            ServerModeService serverModeService,
            OnlinePlayerRepository localOnlineRepository,
            OnlinePlayerRepository redisOnlineRepository
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageService = languageService;
        this.scheduler = scheduler;
        this.redisManager = redisManager;
        this.serverModeService = serverModeService;
        this.localOnlineRepository = localOnlineRepository;
        this.redisOnlineRepository = redisOnlineRepository;
    }

    public void execute(CommandSender sender) {
        UUID senderUuid = sender instanceof Player player ? player.getUniqueId() : null;
        CompletableFuture<List<PatrolTarget>> localFuture = localOnlineRepository.findAllOnline();
        CompletableFuture<List<PatrolTarget>> globalFuture = serverModeService.isCrossServerMode()
                ? redisOnlineRepository.findAllOnline()
                .handle((players, throwable) -> throwable == null ? CompletableFuture.completedFuture(players) : localFuture)
                .thenCompose(future -> future)
                : localFuture;

        localFuture.thenCombine(globalFuture, (localPlayers, globalPlayers) -> new StatusSnapshot(localPlayers, globalPlayers))
                .thenAccept(snapshot -> sendStatus(sender, senderUuid, snapshot))
                .exceptionally(throwable -> {
                    sendStatus(sender, senderUuid, new StatusSnapshot(List.of(), List.of()));
                    return null;
                });
    }

    private void sendStatus(CommandSender sender, UUID senderUuid, StatusSnapshot snapshot) {
        long candidates = snapshot.globalPlayers().stream()
                .filter(player -> !player.bypass())
                .filter(player -> senderUuid == null || !player.uuid().equals(senderUuid))
                .count();

        send(sender, senderUuid, languageService.component("status.header"));
        send(sender, senderUuid, languageService.component("status.version",
                MessagePlaceholder.unparsed("version", plugin.getPluginMeta().getVersion())));
        send(sender, senderUuid, languageService.component("status.server-id",
                MessagePlaceholder.unparsed("server_id", configManager.settings().server().id())));
        send(sender, senderUuid, languageService.component("status.server-alias",
                MessagePlaceholder.unparsed("server_alias", configManager.settings().server().alias())));
        send(sender, senderUuid, languageService.component("status.redis",
                MessagePlaceholder.unparsed("redis_status", redisStatusText())));
        send(sender, senderUuid, languageService.component("status.mode",
                MessagePlaceholder.unparsed("mode", modeText())));
        send(sender, senderUuid, languageService.component("status.local-online",
                MessagePlaceholder.unparsed("local_online", Integer.toString(snapshot.localPlayers().size()))));
        send(sender, senderUuid, languageService.component("status.global-online",
                MessagePlaceholder.unparsed("global_online", Integer.toString(snapshot.globalPlayers().size()))));
        send(sender, senderUuid, languageService.component("status.candidates",
                MessagePlaceholder.unparsed("candidates", Long.toString(candidates))));
        send(sender, senderUuid, languageService.component("status.folia",
                MessagePlaceholder.unparsed("folia", languageService.plain(scheduler.isFolia() ? "status.boolean-yes" : "status.boolean-no"))));
    }

    private String redisStatusText() {
        if (!configManager.settings().redis().enabled()) {
            return languageService.plain("redis.status-disabled");
        }
        return languageService.plain(redisManager.isAvailable() ? "redis.status-ok" : "redis.status-error");
    }

    private String modeText() {
        RunMode mode = serverModeService.currentMode();
        return switch (mode) {
            case CROSS_SERVER -> languageService.plain("mode.cross-server");
            case LOCAL_FALLBACK -> languageService.plain("mode.local-fallback");
            case LOCAL_ONLY -> languageService.plain("mode.local-only");
        };
    }

    private void send(CommandSender sender, UUID senderUuid, Component component) {
        if (senderUuid != null) {
            scheduler.sendMessage(senderUuid, component);
            return;
        }
        scheduler.runGlobal(() -> sender.sendMessage(component));
    }

    private record StatusSnapshot(List<PatrolTarget> localPlayers, List<PatrolTarget> globalPlayers) {
    }
}
