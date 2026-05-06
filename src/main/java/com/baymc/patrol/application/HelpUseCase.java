package com.baymc.patrol.application;

import com.baymc.patrol.lang.LanguageService;
import com.baymc.patrol.scheduler.SchedulerAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 命令帮助用例
 */
public final class HelpUseCase {
    private static final List<String> HELP_KEYS = List.of(
            "help.header",
            "help.next",
            "help.back",
            "help.status",
            "help.reload",
            "help.help"
    );

    private final LanguageService languageService;
    private final SchedulerAdapter scheduler;

    public HelpUseCase(LanguageService languageService, SchedulerAdapter scheduler) {
        this.languageService = languageService;
        this.scheduler = scheduler;
    }

    public void execute(CommandSender sender) {
        UUID senderUuid = sender instanceof Player player ? player.getUniqueId() : null;
        for (String key : HELP_KEYS) {
            send(sender, senderUuid, languageService.component(key));
        }
    }

    private void send(CommandSender sender, UUID senderUuid, Component component) {
        if (senderUuid != null) {
            scheduler.sendMessage(senderUuid, component);
            return;
        }
        sender.sendMessage(component);
    }
}
