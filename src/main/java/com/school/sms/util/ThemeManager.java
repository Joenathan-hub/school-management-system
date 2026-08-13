package com.school.sms.util;

import com.school.sms.dao.SettingsDAO;
import javafx.scene.Scene;

/**
 * Tracks and applies the light/dark theme choice. The choice is stored in
 * the Settings table (same place school name lives), so it persists across
 * restarts and applies from the very first screen (login/setup), not just
 * after logging in.
 */
public class ThemeManager {

    private static final SettingsDAO settingsDAO = new SettingsDAO();

    public static boolean isDarkMode() {
        return "dark".equals(settingsDAO.get("app.theme", "light"));
    }

    public static void setDarkMode(boolean dark) {
        settingsDAO.set("app.theme", dark ? "dark" : "light");
    }

    private static String getStylesheetPath() {
        return isDarkMode() ? "/css/theme-dark.css" : "/css/theme.css";
    }

    /** Clears and re-applies the correct stylesheet to a live Scene — call this after toggling, no rebuild needed. */
    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(getStylesheetPath()).toExternalForm());
    }
}