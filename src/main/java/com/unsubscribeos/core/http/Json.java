package com.unsubscribeos.core.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Thin shared Jackson facade. Tree-model parsing keeps provider mapping explicit and dependency-free. */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {}

    public static JsonNode parse(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON response", e);
        }
    }

    public static <T> T read(String body, Class<T> type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON for " + type.getSimpleName(), e);
        }
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize JSON", e);
        }
    }
}
