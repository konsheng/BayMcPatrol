package com.baymc.patrol.error;

/**
 * 错误报告
 *
 * @param code 错误码
 * @param traceId 追踪 ID
 * @param message 错误摘要
 * @param createdAt 创建时间
 */
public record ErrorReport(ErrorCode code, String traceId, String message, long createdAt) {
}
