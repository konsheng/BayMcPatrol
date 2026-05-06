package com.baymc.patrol.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.baymc.patrol.util.YamlDefaults;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.BiConsumer;

/**
 * 基于 MiniMessage 的语言服务
 */
public final class MiniMessageLanguageService implements LanguageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BiConsumer<String, Integer> completionLogger = (file, count) -> {
    };
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
        int completedKeys = YamlDefaults.mergeMissingKeys(plugin, "lang/zh_CN.yml", langFile);
        language = YamlConfiguration.loadConfiguration(langFile);
        if (completedKeys > 0) {
            completionLogger.accept("lang/zh_CN.yml", completedKeys);
        }
    }

    public void setCompletionLogger(BiConsumer<String, Integer> completionLogger) {
        this.completionLogger = completionLogger;
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
        return key;
    }

    @Override
    public void send(CommandSender sender, String key, MessagePlaceholder... placeholders) {
        sender.sendMessage(component(key, placeholders));
    }
}
