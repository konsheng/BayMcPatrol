package com.baymc.patrol.error;

import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 统一错误处理服务
 */
public final class ErrorService {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final Deque<ErrorReport> reports = new ArrayDeque<>();

    public ErrorService(JavaPlugin plugin, ConfigManager configManager, LanguageService languageService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageService = languageService;
    }

    public synchronized ErrorReport report(ErrorCode code, Throwable throwable) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String message = throwable == null ? code.name() : throwable.getMessage();
        ErrorReport report = new ErrorReport(code, traceId, message == null ? code.name() : message, System.currentTimeMillis());
        reports.addFirst(report);
        while (reports.size() > configManager.settings().runtime().error().keepLastErrors()) {
            reports.removeLast();
        }

        plugin.getServer().getConsoleSender().sendMessage(languageService.component(
                "console.error-summary",
                MessagePlaceholder.unparsed("error", code.name()),
                MessagePlaceholder.unparsed("trace_id", traceId)
        ));

        if (throwable != null && configManager.settings().runtime().error().debug()
                && configManager.settings().runtime().error().printStacktraceToConsole()) {
            plugin.getLogger().log(Level.WARNING, code.name() + " traceId=" + traceId, throwable);
        }
        return report;
    }

    public synchronized Optional<ErrorReport> lastError() {
        return Optional.ofNullable(reports.peekFirst());
    }
}
