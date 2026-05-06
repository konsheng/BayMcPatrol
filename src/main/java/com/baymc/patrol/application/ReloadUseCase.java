package com.baymc.patrol.application;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.error.ErrorCode;
import com.baymc.patrol.error.ErrorReport;
import com.baymc.patrol.error.ErrorService;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.redis.RedisManager;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.RunMode;
import com.baymc.patrol.service.ServerModeService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 插件重载用例
 */
public final class ReloadUseCase {
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final ErrorService errorService;
    private final SchedulerAdapter scheduler;
    private final RedisManager redisManager;
    private final ServerModeService serverModeService;
    private final Runnable reloadOperation;

    public ReloadUseCase(
            ConfigManager configManager,
            LanguageService languageService,
            ErrorService errorService,
            SchedulerAdapter scheduler,
            RedisManager redisManager,
            ServerModeService serverModeService,
            Runnable reloadOperation
    ) {
        this.configManager = configManager;
        this.languageService = languageService;
        this.errorService = errorService;
        this.scheduler = scheduler;
        this.redisManager = redisManager;
        this.serverModeService = serverModeService;
        this.reloadOperation = reloadOperation;
    }

    public void execute(CommandSender sender) {
        try {
            reloadOperation.run();
            send(sender, languageService.component("command.reload-success"));
            send(sender, languageService.component("command.reload-status",
                    MessagePlaceholder.unparsed("redis_status", redisStatusText()),
                    MessagePlaceholder.unparsed("mode", modeText())));
        } catch (Exception exception) {
            ErrorReport report = errorService.report(ErrorCode.INTERNAL_ERROR, exception);
            send(sender, languageService.component("error.internal",
                    MessagePlaceholder.unparsed("trace_id", report.traceId())));
        }
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

    private void send(CommandSender sender, Component component) {
        if (sender instanceof Player player) {
            scheduler.sendMessage(player.getUniqueId(), component);
            return;
        }
        sender.sendMessage(component);
    }
}
