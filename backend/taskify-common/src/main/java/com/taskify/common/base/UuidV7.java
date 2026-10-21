package com.taskify.common.base;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 生成器：时间有序，利于索引局部性与分页，且分布式无冲突。
 * Java 21 未内置 UUID v7，故按 RFC 9562 手工实现。
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /** 生成一个 UUID v7 并返回其标准字符串表示（含连字符）。 */
    public static String randomUuid() {
        long msb = 0L;
        long lsb = 0L;

        long timestamp = System.currentTimeMillis();
        // 48-bit 时间戳（毫秒），置于高位
        msb |= (timestamp & 0x0000_FFFF_FFFF_FFFFL) << 16;
        // 版本号 7
        msb |= 0x0000_0000_0000_7000L;
        // 随机低 12 位
        msb |= RANDOM.nextLong() & 0x0000_0000_0000_0FFFL;

        // 变体位 10xx
        lsb |= 0x8000_0000_0000_0000L;
        // 随机低 62 位
        lsb |= RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;

        return new UUID(msb, lsb).toString();
    }
}
