package com.baymc.patrol.bootstrap;

import com.baymc.patrol.application.PatrolBackUseCase;
import com.baymc.patrol.application.PatrolNextUseCase;
import com.baymc.patrol.application.ReloadUseCase;
import com.baymc.patrol.application.StatusUseCase;
import com.baymc.patrol.application.HelpUseCase;
import com.baymc.patrol.command.BayMcPatrolCommand;
import com.baymc.patrol.command.BayMcPatrolTabCompleter;
import com.baymc.patrol.config.ConfigManager;
import com.baymc.patrol.domain.TargetSelector;
import com.baymc.patrol.error.ErrorService;
import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.lang.MessagePlaceholder;
import com.baymc.patrol.lang.MiniMessageLanguageService;
import com.baymc.patrol.listener.PlayerJoinListener;
import com.baymc.patrol.listener.PlayerQuitListener;
import com.baymc.patrol.local.LocalActivePatrolRepository;
import com.baymc.patrol.local.LocalLastPatrolRepository;
import com.baymc.patrol.local.LocalOnlinePlayerRepository;
import com.baymc.patrol.local.LocalPatrolSessionRepository;
import com.baymc.patrol.redis.RedisActivePatrolRepository;
import com.baymc.patrol.redis.RedisKey;
import com.baymc.patrol.redis.RedisLastPatrolRepository;
import com.baymc.patrol.redis.RedisManager;
import com.baymc.patrol.redis.RedisOnlinePlayerRepository;
import com.baymc.patrol.redis.RedisPatrolSessionRepository;
import com.baymc.patrol.redis.RedisPendingPatrolRepository;
import com.baymc.patrol.scheduler.FoliaSchedulerAdapter;
import com.baymc.patrol.scheduler.PaperSchedulerAdapter;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import com.baymc.patrol.service.OnlineSyncService;
import com.baymc.patrol.service.PatrolRepositories;
import com.baymc.patrol.service.PendingTeleportService;
import com.baymc.patrol.service.RepositoryProvider;
import com.baymc.patrol.service.ServerModeService;
import com.baymc.patrol.service.TeleportService;
import com.baymc.patrol.util.DurationFormatter;
import com.baymc.patrol.util.JsonCodec;
import com.baymc.patrol.velocity.VelocityConnectService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 组合根
 */
public final class CompositionRoot {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LanguageService languageService;
    private final SchedulerAdapter scheduler;
    private final RedisManager redisManager;
    private final ServerModeService serverModeService;
    private final OnlineSyncService onlineSyncService;
    private final BayMcPatrolCommand command;
    private final BayMcPatrolTabCompleter tabCompleter;
    private final PlayerJoinListener playerJoinListener;
    private final PlayerQuitListener playerQuitListener;

    public CompositionRoot(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager(plugin);
        MiniMessageLanguageService miniMessageLanguageService = new MiniMessageLanguageService(plugin);
        this.languageService = miniMessageLanguageService;
        miniMessageLanguageService.setCompletionLogger((file, count) -> plugin.getServer().getConsoleSender().sendMessage(this.languageService.component(
                "console.lang-completed",
                MessagePlaceholder.unparsed("file", file),
                MessagePlaceholder.unparsed("count", Integer.toString(count))
        )));
        this.languageService.reload();
        this.configManager.setCompletionLogger((file, count) -> plugin.getServer().getConsoleSender().sendMessage(this.languageService.component(
                "console.config-completed",
                MessagePlaceholder.unparsed("file", file),
                MessagePlaceholder.unparsed("count", Integer.toString(count))
        )));
        this.configManager.load();
        this.scheduler = createScheduler(plugin);

        ErrorService errorService = new ErrorService(plugin, configManager, languageService);
        JsonCodec jsonCodec = new JsonCodec();
        RedisKey redisKey = new RedisKey(configManager);
        this.redisManager = new RedisManager(plugin, configManager, languageService, errorService, scheduler);

        RedisOnlinePlayerRepository redisOnlineRepository = new RedisOnlinePlayerRepository(configManager, redisManager, redisKey);
        RedisPatrolSessionRepository redisSessionRepository = new RedisPatrolSessionRepository(configManager, redisManager, redisKey, jsonCodec);
        RedisActivePatrolRepository redisActiveRepository = new RedisActivePatrolRepository(configManager, redisManager, redisKey);
        RedisPendingPatrolRepository pendingRepository = new RedisPendingPatrolRepository(configManager, redisManager, redisKey, jsonCodec);
        RedisLastPatrolRepository redisLastPatrolRepository = new RedisLastPatrolRepository(redisManager, redisKey);

        LocalOnlinePlayerRepository localOnlineRepository = new LocalOnlinePlayerRepository(configManager, scheduler);
        LocalPatrolSessionRepository localSessionRepository = new LocalPatrolSessionRepository(configManager);
        LocalActivePatrolRepository localActiveRepository = new LocalActivePatrolRepository(configManager);
        LocalLastPatrolRepository localLastPatrolRepository = new LocalLastPatrolRepository();

        this.serverModeService = new ServerModeService(configManager, redisManager);
        RepositoryProvider repositoryProvider = new RepositoryProvider(
                serverModeService,
                new PatrolRepositories(redisOnlineRepository, redisSessionRepository, redisActiveRepository, redisLastPatrolRepository, true),
                new PatrolRepositories(localOnlineRepository, localSessionRepository, localActiveRepository, localLastPatrolRepository, false)
        );

        TeleportService teleportService = new TeleportService(scheduler);
        VelocityConnectService velocityConnectService = new VelocityConnectService(configManager, scheduler);
        TargetSelector targetSelector = new TargetSelector();
        DurationFormatter durationFormatter = new DurationFormatter(languageService);

        PatrolNextUseCase patrolNextUseCase = new PatrolNextUseCase(
                configManager,
                languageService,
                errorService,
                scheduler,
                repositoryProvider,
                pendingRepository,
                teleportService,
                velocityConnectService,
                targetSelector,
                durationFormatter
        );
        PatrolBackUseCase patrolBackUseCase = new PatrolBackUseCase(
                configManager,
                languageService,
                errorService,
                scheduler,
                repositoryProvider,
                pendingRepository,
                teleportService,
                velocityConnectService
        );
        StatusUseCase statusUseCase = new StatusUseCase(
                plugin,
                configManager,
                languageService,
                scheduler,
                redisManager,
                serverModeService,
                localOnlineRepository,
                redisOnlineRepository
        );
        ReloadUseCase reloadUseCase = new ReloadUseCase(
                configManager,
                languageService,
                errorService,
                scheduler,
                redisManager,
                serverModeService,
                this::reloadRuntime
        );
        HelpUseCase helpUseCase = new HelpUseCase(languageService, scheduler);

        PendingTeleportService pendingTeleportService = new PendingTeleportService(
                configManager,
                languageService,
                errorService,
                scheduler,
                redisManager,
                localOnlineRepository,
                redisOnlineRepository,
                redisSessionRepository,
                redisActiveRepository,
                pendingRepository,
                redisLastPatrolRepository,
                teleportService,
                velocityConnectService,
                durationFormatter
        );

        this.onlineSyncService = new OnlineSyncService(plugin, configManager, languageService, redisManager, scheduler, redisOnlineRepository);
        this.command = new BayMcPatrolCommand(languageService, patrolNextUseCase, patrolBackUseCase, statusUseCase, reloadUseCase, helpUseCase);
        this.tabCompleter = new BayMcPatrolTabCompleter();
        this.playerJoinListener = new PlayerJoinListener(onlineSyncService, pendingTeleportService);
        this.playerQuitListener = new PlayerQuitListener(scheduler, onlineSyncService);
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public LanguageService languageService() {
        return languageService;
    }

    public SchedulerAdapter scheduler() {
        return scheduler;
    }

    public RedisManager redisManager() {
        return redisManager;
    }

    public ServerModeService serverModeService() {
        return serverModeService;
    }

    public OnlineSyncService onlineSyncService() {
        return onlineSyncService;
    }

    public BayMcPatrolCommand command() {
        return command;
    }

    public BayMcPatrolTabCompleter tabCompleter() {
        return tabCompleter;
    }

    public PlayerJoinListener playerJoinListener() {
        return playerJoinListener;
    }

    public PlayerQuitListener playerQuitListener() {
        return playerQuitListener;
    }

    public void reloadRuntime() {
        configManager.reload();
        languageService.reload();
        registerPluginChannel();
        redisManager.connect();
        onlineSyncService.start();
        onlineSyncService.refresh();
    }

    public void registerPluginChannel() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        if (!configManager.settings().velocity().enabled()) {
            return;
        }
        String channel = configManager.settings().velocity().channel();
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getConsoleSender().sendMessage(languageService.component(
                "console.registered-channel",
                MessagePlaceholder.unparsed("channel", channel)
        ));
    }

    private SchedulerAdapter createScheduler(JavaPlugin plugin) {
        if (isFoliaRuntime()) {
            return new FoliaSchedulerAdapter(plugin);
        }
        return new PaperSchedulerAdapter(plugin);
    }

    private boolean isFoliaRuntime() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
