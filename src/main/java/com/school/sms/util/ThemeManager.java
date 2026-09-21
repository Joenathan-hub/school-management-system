package com.school.sms.util;

import com.school.sms.dao.SettingsDAO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Window;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Owns everything to do with <em>which</em> stylesheets are on a scene and where
 * the user's choices about appearance are stored.
 *
 * <h2>The two-layer stylesheet model</h2>
 * There is one structural layer and one token layer:
 * <ul>
 *   <li>{@code /css/structure.css} + {@code /css/components.css} — layout, spacing,
 *       type, radii. Neither file contains a single colour literal.</li>
 *   <li>{@code /css/tokens-light.css} <em>or</em> {@code /css/tokens-dark.css} — the
 *       only files in the project that contain colours. Exactly one of them is
 *       attached to a scene at a time.</li>
 * </ul>
 * Switching light &#8596; dark therefore replaces exactly one stylesheet and there is
 * no per-component conditional logic anywhere in the application.
 *
 * <p>AtlantaFX (Primer) is installed as the <em>user agent</em> stylesheet, so
 * controls this design does not restyle itself — scrollbars, combo box popups,
 * context menus, tooltips, table headers, spinners, date pickers, focus rings —
 * inherit their structure from there. The token sheets redirect AtlantaFX's own
 * colour variables at this palette, so those controls are painted in the
 * application's colours rather than in Primer's.</p>
 *
 * <h2>Persistence</h2>
 * The three-state theme choice and the rail's collapsed state live in the JavaFX
 * preference store ({@link Preferences}), not the database, so they apply from the
 * very first window — before any connection to the Access database is open — and
 * are per user account on the machine. Earlier versions of the application stored
 * a two-state theme in the settings table; that value is read once, migrated, and
 * then ignored.
 */
public final class ThemeManager {

    /** Bundled UI family. Faces are loaded in {@link #bootstrap()} from /fonts. */
    public static final String FONT_FAMILY = "Inter";

    /**
     * Families used for anything numeric. Every digit in these faces has the same
     * advance width, so columns of money and percentages line up on the decimal
     * point without the layout having to pad strings.
     */
    public static final String TABULAR_FONT_FAMILY = "Inter Tabular";

    private static final String[] FONT_RESOURCES = {
        "/fonts/Inter-Regular.ttf",
        "/fonts/Inter-Medium.ttf",
        "/fonts/Inter-SemiBold.ttf",
        "/fonts/Inter-Bold.ttf",
        "/fonts/Inter-ExtraBold.ttf",
        "/fonts/InterTabular-SemiBold.ttf",
        "/fonts/InterTabular-Bold.ttf",
        "/fonts/InterTabular-ExtraBold.ttf"
    };

    /** Structural layer: identical in both themes, no colours. */
    private static final String[] STRUCTURAL_SHEETS = {
        "/css/structure.css",
        "/css/components.css"
    };

    /** Token layer: exactly one of these is attached at a time. */
    private static final String TOKEN_SHEET_LIGHT = "/css/tokens-light.css";
    private static final String TOKEN_SHEET_DARK = "/css/tokens-dark.css";

    private static final String PREF_NODE = "com/school/sms/ui";
    private static final String PREF_MODE = "theme.mode";
    private static final String PREF_RAIL_COLLAPSED = "rail.collapsed";
    private static final String LEGACY_SETTINGS_KEY = "app.theme";

    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private static Preferences prefs;
    private static ThemeMode mode;
    private static boolean modeLoaded;
    private static Boolean osPrefersDark;
    private static boolean bootstrapped;
    private static boolean fontsLoaded;

    private ThemeManager() {
    }

    // ------------------------------------------------------------------
    // Bootstrap
    // ------------------------------------------------------------------

    /**
     * Call once, from {@code Application.start}, before the first window is shown.
     * Loads the bundled fonts, installs the base theme as the user agent
     * stylesheet, and starts watching for windows being opened so that every
     * dialog and secondary stage gets the identical stylesheet set.
     */
    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        loadFonts();
        installBaseTheme();
        installWindowWatcher();
        detectOperatingSystemTheme();
    }

    /**
     * Registers the bundled Inter faces with the FX toolkit. Faces are loaded by
     * resource rather than through {@code @font-face} because JavaFX's
     * {@code @font-face} support carries no weight or style metadata, which would
     * collapse all five weights of a family onto one face.
     *
     * <p>If the resources are missing the application still runs: every rule in
     * structure.css names a fallback chain after "Inter" (system UI, Segoe UI,
     * Roboto, Arial), so text falls back rather than disappearing.</p>
     */
    private static void loadFonts() {
        if (fontsLoaded) {
            return;
        }
        fontsLoaded = true;
        for (String resource : FONT_RESOURCES) {
            try (InputStream in = ThemeManager.class.getResourceAsStream(resource)) {
                if (in != null) {
                    Font.loadFont(in, 12.5);
                }
            } catch (Exception ignored) {
                // A font that will not load is not fatal — the fallback chain covers it.
            }
        }
    }

    /** AtlantaFX supplies the user agent (base) stylesheet. */
    private static void installBaseTheme() {
        try {
            String sheet = new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet();
            Application.setUserAgentStylesheet(sheet);
        } catch (Throwable ignored) {
            // Without the base theme the application is still fully styled by its
            // own two layers; unstyled controls simply fall back to Modena.
        }
    }

    /**
     * Every window opened after startup — alerts, confirmation dialogs, printing
     * previews, secondary stages — is given the same stylesheet set. JavaFX has no
     * global hook for this, but the stage window list is observable, so watching it
     * is equivalent and cannot be forgotten by an individual screen.
     */
    private static void installWindowWatcher() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window.getScene() != null) {
                        applyTheme(window.getScene());
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Applying stylesheets
    // ------------------------------------------------------------------

    /**
     * Attaches the structural layer plus the token sheet for the active theme.
     * Safe to call on any scene at any time; calling it again after a theme change
     * is all that a live window needs to repaint.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        loadFonts();
        List<String> sheets = new ArrayList<>();
        for (String sheet : STRUCTURAL_SHEETS) {
            String url = url(sheet);
            if (url != null) {
                sheets.add(url);
            }
        }
        String tokenUrl = url(isDark() ? TOKEN_SHEET_DARK : TOKEN_SHEET_LIGHT);
        if (tokenUrl != null) {
            sheets.add(tokenUrl);
        }
        scene.getStylesheets().setAll(sheets);
    }

    /** Re-applies the stylesheets to every open window. */
    public static void applyToAllWindows() {
        for (Window window : Window.getWindows()) {
            if (window.getScene() != null) {
                applyTheme(window.getScene());
            }
        }
    }

    private static String url(String resource) {
        java.net.URL url = ThemeManager.class.getResource(resource);
        return url == null ? null : url.toExternalForm();
    }

    // ------------------------------------------------------------------
    // Theme choice
    // ------------------------------------------------------------------

    public static ThemeMode getMode() {
        if (!modeLoaded) {
            modeLoaded = true;
            mode = ThemeMode.fromName(prefs().get(PREF_MODE, migrateLegacyMode()));
        }
        return mode;
    }

    public static void setMode(ThemeMode newMode) {
        if (newMode == null) {
            return;
        }
        getMode();
        mode = newMode;
        try {
            prefs().put(PREF_MODE, newMode.name());
        } catch (Exception ignored) {
            // An unwritable preference store only costs us persistence, not function.
        }
        if (newMode == ThemeMode.SYSTEM) {
            detectOperatingSystemTheme();
        }
        // Repaint even when the resolved appearance did not change: the rail's
        // theme control shows which of the three states is selected.
        applyToAllWindows();
        fireChanged();
    }

    /** Shifts LIGHT &#8594; DARK &#8594; SYSTEM &#8594; LIGHT. Used by the rail's theme control. */
    public static void cycleMode() {
        switch (getMode()) {
            case LIGHT -> setMode(ThemeMode.DARK);
            case DARK -> setMode(ThemeMode.SYSTEM);
            case SYSTEM -> setMode(ThemeMode.LIGHT);
        }
    }

    /** True when the application should currently paint dark. */
    public static boolean isDark() {
        return switch (getMode()) {
            case DARK -> true;
            case LIGHT -> false;
            case SYSTEM -> Boolean.TRUE.equals(osPrefersDark);
        };
    }

    /** Kept for callers written before the three-state theme existed. */
    public static boolean isDarkMode() {
        return isDark();
    }

    /** Kept for callers written before the three-state theme existed. */
    public static void setDarkMode(boolean dark) {
        setMode(dark ? ThemeMode.DARK : ThemeMode.LIGHT);
    }

    /**
     * Reads the two-state theme this application used to keep in the settings
     * table, so the first run after upgrading keeps the user's choice. Returns null
     * when there is nothing to migrate (the common case).
     */
    private static String migrateLegacyMode() {
        try {
            String legacy = new SettingsDAO().get(LEGACY_SETTINGS_KEY);
            if (legacy == null) {
                return null;
            }
            String migrated = "dark".equalsIgnoreCase(legacy) ? ThemeMode.DARK.name() : ThemeMode.LIGHT.name();
            try {
                prefs().put(PREF_MODE, migrated);
            } catch (Exception ignored) {
                // Migration is best-effort.
            }
            return migrated;
        } catch (Throwable ignored) {
            // No database yet (first run) or the driver is unavailable: nothing to migrate.
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Operating system preference (only consulted in SYSTEM mode)
    // ------------------------------------------------------------------

    /**
     * Asks the desktop what it is set to, on a background thread, and repaints if
     * the answer disagrees with what is currently on screen. Every probe is
     * best-effort: an unknown platform, a missing command or a timeout simply
     * leaves the resolved theme as light.
     */
    private static void detectOperatingSystemTheme() {
        Thread probe = new Thread(() -> {
            Boolean dark = probeOperatingSystemDark();
            if (dark == null) {
                return;
            }
            boolean changed = !dark.equals(osPrefersDark);
            osPrefersDark = dark;
            if (changed && getMode() == ThemeMode.SYSTEM) {
                Platform.runLater(() -> {
                    applyToAllWindows();
                    fireChanged();
                });
            }
        }, "theme-os-probe");
        probe.setDaemon(true);
        probe.start();
    }

    /** @return TRUE for a dark desktop, FALSE for a light one, null when unknowable. */
    static Boolean probeOperatingSystemDark() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                String out = runFor("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                return out == null ? null : out.contains("0x0");
            }
            if (os.contains("mac")) {
                String out = runFor("defaults", "read", "-g", "AppleInterfaceStyle");
                return out == null ? null : out.toLowerCase(Locale.ROOT).contains("dark");
            }
            if (os.contains("linux") || os.contains("unix")) {
                String scheme = runFor("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
                if (scheme != null && scheme.toLowerCase(Locale.ROOT).contains("dark")) {
                    return true;
                }
                if (scheme != null && scheme.toLowerCase(Locale.ROOT).contains("light")) {
                    return false;
                }
                String gtk = runFor("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
                return gtk == null ? null : gtk.toLowerCase(Locale.ROOT).contains("dark");
            }
        } catch (Throwable ignored) {
            // Any failure at all means "unknown", which resolves to light.
        }
        return null;
    }

    private static String runFor(String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                return null;
            }
            return new String(process.getInputStream().readAllBytes()).trim();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    // ------------------------------------------------------------------
    // Rail collapse state
    // ------------------------------------------------------------------

    public static boolean isRailCollapsed() {
        try {
            return prefs().getBoolean(PREF_RAIL_COLLAPSED, false);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void setRailCollapsed(boolean collapsed) {
        try {
            prefs().putBoolean(PREF_RAIL_COLLAPSED, collapsed);
        } catch (Exception ignored) {
            // Persistence is best-effort; the rail still collapses for this session.
        }
        fireChanged();
    }

    // ------------------------------------------------------------------
    // Change notification
    // ------------------------------------------------------------------

    /** Registers a callback fired whenever the theme or rail state changes. */
    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    private static void fireChanged() {
        for (Runnable listener : new ArrayList<>(LISTENERS)) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // One misbehaving listener must not stop the others.
            }
        }
    }

    private static Preferences prefs() {
        if (prefs == null) {
            prefs = Preferences.userRoot().node(PREF_NODE);
        }
        return prefs;
    }
}
