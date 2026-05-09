package com.voting.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60;
    private static final Map<String, AttemptWindow> ATTEMPTS = new ConcurrentHashMap<>();

    private LoginRateLimiter() {
    }

    public static boolean isBlocked(String key) {
        AttemptWindow window = ATTEMPTS.get(key);
        if (window == null || window.isExpired()) {
            ATTEMPTS.remove(key);
            return false;
        }
        return window.count >= MAX_ATTEMPTS;
    }

    public static void recordFailure(String key) {
        ATTEMPTS.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new AttemptWindow(1, Instant.now().plusSeconds(WINDOW_SECONDS).toEpochMilli());
            }
            existing.count++;
            return existing;
        });
    }

    public static void recordSuccess(String key) {
        ATTEMPTS.remove(key);
    }

    private static final class AttemptWindow {
        private int count;
        private final long expiresAt;

        private AttemptWindow(int count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
