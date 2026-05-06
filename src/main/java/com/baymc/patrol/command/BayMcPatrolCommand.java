package com.baymc.patrol.command;

import com.baymc.patrol.application.HelpUseCase;
import com.baymc.patrol.application.PatrolBackUseCase;
import com.baymc.patrol.application.PatrolNextUseCase;
import com.baymc.patrol.application.ReloadUseCase;
import com.baymc.patrol.application.StatusUseCase;
import com.baymc.patrol.lang.LanguageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 主命令入口
 */
public final class BayMcPatrolCommand implements CommandExecutor {
    private final LanguageService languageService;
    private final PatrolNextUseCase patrolNextUseCase;
    private final PatrolBackUseCase patrolBackUseCase;
    private final StatusUseCase statusUseCase;
    private final ReloadUseCase reloadUseCase;
    private final HelpUseCase helpUseCase;

    public BayMcPatrolCommand(
            LanguageService languageService,
            PatrolNextUseCase patrolNextUseCase,
            PatrolBackUseCase patrolBackUseCase,
            StatusUseCase statusUseCase,
            ReloadUseCase reloadUseCase,
            HelpUseCase helpUseCase
    ) {
        this.languageService = languageService;
        this.patrolNextUseCase = patrolNextUseCase;
        this.patrolBackUseCase = patrolBackUseCase;
        this.statusUseCase = statusUseCase;
        this.reloadUseCase = reloadUseCase;
        this.helpUseCase = helpUseCase;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String subCommand = args.length == 0 ? "next" : args[0].toLowerCase();
        switch (subCommand) {
            case "next" -> handleNext(sender);
            case "back" -> handleBack(sender);
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "help" -> helpUseCase.execute(sender);
            default -> languageService.send(sender, "command.invalid-command");
        }
        return true;
    }

    private void handleNext(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "command.player-required");
            return;
        }
        if (!sender.hasPermission("baymcpatrol.use")) {
            languageService.send(sender, "command.no-permission");
            return;
        }
        patrolNextUseCase.execute(player.getUniqueId(), player.getName());
    }

    private void handleBack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            languageService.send(sender, "command.player-required");
            return;
        }
        if (!sender.hasPermission("baymcpatrol.back")) {
            languageService.send(sender, "command.no-permission");
            return;
        }
        patrolBackUseCase.execute(player.getUniqueId(), player.getName());
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("baymcpatrol.status")) {
            languageService.send(sender, "command.no-permission");
            return;
        }
        statusUseCase.execute(sender);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("baymcpatrol.reload")) {
            languageService.send(sender, "command.no-permission");
            return;
        }
        reloadUseCase.execute(sender);
    }
}
