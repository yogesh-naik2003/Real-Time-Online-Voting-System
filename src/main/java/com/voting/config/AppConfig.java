package com.voting.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application.properties not found");
            }
            PROPERTIES.load(inputStream);
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String get(String key, String environmentKey) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        String propertyValue = PROPERTIES.getProperty(key);
        return propertyValue == null ? null : propertyValue.trim();
    }
}
