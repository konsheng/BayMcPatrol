package com.baymc.patrol.error;

/**
 * 插件业务异常
 */
public final class PluginException extends RuntimeException {
    private final ErrorCode code;

    public PluginException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public PluginException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
