package com.baymc.patrol.domain;

/**
 * 巡查业务结果
 *
 * @param success 是否成功
 * @param messageKey 结果消息语言键
 */
public record PatrolResult(boolean success, String messageKey) {
    public static PatrolResult success(String messageKey) {
        return new PatrolResult(true, messageKey);
    }

    public static PatrolResult failure(String messageKey) {
        return new PatrolResult(false, messageKey);
    }
}
