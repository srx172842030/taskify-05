package com.taskify.common.cache;

/**
 * Redis 键命名约定（统一前缀，便于清理与排查）。
 */
public final class RedisKeys {

    private static final String CAPTCHA_PREFIX = "taskify:captcha:";

    private RedisKeys() {
    }

    public static String captcha(String id) {
        return CAPTCHA_PREFIX + id;
    }
}
