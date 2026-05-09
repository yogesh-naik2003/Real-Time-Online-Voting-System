package com.voting.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password == null ? "" : password, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.trim().isEmpty()) {
            return false;
        }
        String candidate = password == null ? "" : password;
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return BCrypt.checkpw(candidate, storedHash);
        }
        return storedHash.equals(legacySha256(candidate));
    }

    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !storedHash.startsWith("$2");
    }

    private static String legacySha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 hashing unavailable", ex);
        }
    }
}
