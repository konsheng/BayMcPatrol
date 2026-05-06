package com.baymc.patrol.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * JSON 编解码工具
 */
public final class JsonCodec {
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    public <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }
}
