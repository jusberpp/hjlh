package com.huijulh.study.common;

import org.springframework.jdbc.support.KeyHolder;

public final class GeneratedKeys {
    private GeneratedKeys() {}

    public static long require(KeyHolder keyHolder, String column) {
        if (keyHolder.getKeys() != null) {
            Object value = keyHolder.getKeys().get(column);
            if (value == null) value = keyHolder.getKeys().get(column.toUpperCase());
            if (value instanceof Number number) return number.longValue();
        }
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Database did not return generated key " + column);
        return key.longValue();
    }
}
