package com.baymc.patrol;

import com.baymc.patrol.bootstrap.PluginLifecycleManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BayMcPatrol 插件主类
 */
public final class BayMcPatrolPlugin extends JavaPlugin {
    private PluginLifecycleManager lifecycleManager;

    @Override
    public void onEnable() {
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
