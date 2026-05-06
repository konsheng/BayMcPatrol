package com.baymc.patrol.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 主命令补全
 */
public final class BayMcPatrolTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        addIfMatches(completions, "help", prefix);
        addIfAllowed(sender, completions, "next", "baymcpatrol.use", prefix);
        addIfAllowed(sender, completions, "back", "baymcpatrol.back", prefix);
        addIfAllowed(sender, completions, "status", "baymcpatrol.status", prefix);
        addIfAllowed(sender, completions, "reload", "baymcpatrol.reload", prefix);
        return completions;
    }

    private void addIfAllowed(CommandSender sender, List<String> completions, String value, String permission, String prefix) {
        if (sender.hasPermission(permission) && value.startsWith(prefix)) {
            completions.add(value);
        }
    }

    private void addIfMatches(List<String> completions, String value, String prefix) {
        if (value.startsWith(prefix)) {
            completions.add(value);
        }
    }
}
