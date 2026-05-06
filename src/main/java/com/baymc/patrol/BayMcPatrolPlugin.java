package com.baymc.patrol;

import com.baymc.patrol.bootstrap.PluginLifecycleManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BayMcPatrol 插件主类
 */
public final class BayMcPatrolPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 31135;
    private PluginLifecycleManager lifecycleManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        lifecycleManager = new PluginLifecycleManager(this);
        lifecycleManager.enable();
    }

    @Override
    public void onDisable() {
        if (lifecycleManager != null) {
            lifecycleManager.disable();
        }
    }
}
