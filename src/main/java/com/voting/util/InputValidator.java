package com.voting.util;

import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOBILE = Pattern.compile("^[0-9]{10}$");
    private static final Pattern VOTER_ID = Pattern.compile("^[A-Z0-9-]{4,30}$");

    private InputValidator() {
    }

    public static String required(String value, String fieldName, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long.");
        }
        return trimmed;
    }

    public static String email(String value) {
        String email = required(value, "Email", 120).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        return email;
    }

    public static String mobile(String value) {
        String mobile = required(value, "Mobile number", 20);
        if (!MOBILE.matcher(mobile).matches()) {
            throw new IllegalArgumentException("Mobile number must contain exactly 10 digits.");
        }
        return mobile;
    }

    public static String voterId(String value) {
        String voterId = required(value, "Voter ID number", 50).toUpperCase();
        if (!VOTER_ID.matcher(voterId).matches()) {
            throw new IllegalArgumentException("Voter ID can contain only letters, numbers, and hyphens.");
        }
        return voterId;
    }

    public static void strongPassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include uppercase, lowercase, and number.");
        }
    }
}
