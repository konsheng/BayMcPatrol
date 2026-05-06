package com.baymc.patrol.bootstrap;

import com.baymc.patrol.lang.MessagePlaceholder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 插件生命周期管理器
 */
public final class PluginLifecycleManager {
    private final JavaPlugin plugin;
    private CompositionRoot root;

    public PluginLifecycleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        root = new CompositionRoot(plugin);
        root.languageService().send(plugin.getServer().getConsoleSender(), "console.config-loaded");
        root.languageService().send(plugin.getServer().getConsoleSender(), "console.lang-loaded",
                MessagePlaceholder.unparsed("language", root.configManager().settings().language().defaultLanguage()));
        root.languageService().send(plugin.getServer().getConsoleSender(),
                root.scheduler().isFolia() ? "console.folia-detected" : "console.paper-detected");

        registerCommand();
        plugin.getServer().getPluginManager().registerEvents(root.playerJoinListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(root.playerQuitListener(), plugin);
        root.registerPluginChannel();
        root.redisManager().connect();
        root.onlineSyncService().start();
        root.onlineSyncService().refresh();
        root.languageService().send(plugin.getServer().getConsoleSender(), "console.enabled");
    }

    public void disable() {
        if (root == null) {
            return;
        }
        root.onlineSyncService().stop();
        root.redisManager().close();
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        root.scheduler().shutdown();
        root.languageService().send(plugin.getServer().getConsoleSender(), "console.disabled");
    }

    private void registerCommand() {
        PluginCommand pluginCommand = plugin.getCommand("baymcpatrol");
        if (pluginCommand == null) {
            throw new IllegalStateException("Command baymcpatrol is not registered");
        }
        pluginCommand.setExecutor(root.command());
        pluginCommand.setTabCompleter(root.tabCompleter());
    }
}
