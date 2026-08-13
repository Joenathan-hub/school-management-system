package com.school.sms.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from src/main/resources/config.properties.
 * Keep real API keys out of version control — add config.properties to
 * .gitignore and commit config.properties.example instead.
 */
public class AppConfig {

    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            } else {
                System.err.println("config.properties not found on classpath — using defaults/empty values.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load config.properties: " + e.getMessage());
        }
        loaded = true;
    }

    public static String get(String key, String defaultValue) {
        ensureLoaded();
        return PROPS.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return get(key, "");
    }
}
