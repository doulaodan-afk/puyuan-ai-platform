package com.puyuanmaoshan.platform.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class BizNoUtil {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private BizNoUtil() {
    }

    public static String nextNo(String prefix) {
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + LocalDateTime.now().format(TS_FORMATTER) + random;
    }
}
