package com.baymc.patrol.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * 基于 MiniMessage 的语言服务
 */
public final class MiniMessageLanguageService implements LanguageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile YamlConfiguration language;

    public MiniMessageLanguageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        File langFile = new File(plugin.getDataFolder(), "lang/zh_CN.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/zh_CN.yml", false);
        }
        language = YamlConfiguration.loadConfiguration(langFile);
    }

    @Override
    public Component component(String key, MessagePlaceholder... placeholders) {
        String template = raw(key);
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(Placeholder.parsed("prefix", raw("prefix")));
        for (MessagePlaceholder placeholder : placeholders) {
            if (placeholder.parsed()) {
                builder.resolver(Placeholder.parsed(placeholder.name(), placeholder.value()));
            } else {
                builder.resolver(Placeholder.unparsed(placeholder.name(), placeholder.value()));
            }
        }
        return miniMessage.deserialize(template, builder.build());
    }

    @Override
    public String plain(String key, MessagePlaceholder... placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, placeholders));
    }

    @Override
    public String raw(String key) {
        String value = language.getString(key);
        if (value != null) {
            return value;
        }
        if ("system.missing-key".equals(key)) {
            return "<dark_gray>[<green>BayMcPatrol</green>]</dark_gray> <red>缺失语言项: <key>";
        }
        return raw("system.missing-key").replace("<key>", key);
    }

    @Override
    public void send(CommandSender sender, String key, MessagePlaceholder... placeholders) {
        sender.sendMessage(component(key, placeholders));
    }
}
