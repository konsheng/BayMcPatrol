package com.baymc.patrol.lang;

/**
 * 语言变量
 *
 * @param name 变量名
 * @param value 变量值
 * @param parsed 是否按 MiniMessage 解析
 */
public record MessagePlaceholder(String name, String value, boolean parsed) {
    public static MessagePlaceholder parsed(String name, String value) {
        return new MessagePlaceholder(name, value, true);
    }

    public static MessagePlaceholder unparsed(String name, String value) {
        return new MessagePlaceholder(name, value, false);
    }
}
