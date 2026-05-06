package com.baymc.patrol.lang;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * 语言服务
 */
public interface LanguageService {
    void reload();

    Component component(String key, MessagePlaceholder... placeholders);

    String plain(String key, MessagePlaceholder... placeholders);

    String raw(String key);

    void send(CommandSender sender, String key, MessagePlaceholder... placeholders);
}
