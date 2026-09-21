package com.school.sms.util;

/**
 * The three theme states the user can choose between.
 *
 * <p>LIGHT and DARK are explicit choices. SYSTEM follows the operating system's
 * own light/dark preference, and re-resolves if the user picks SYSTEM again
 * (for example after changing the desktop theme) — see
 * {@link ThemeManager#probeOperatingSystemDark()}.</p>
 */
public enum ThemeMode {

    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System");

    private final String label;

    ThemeMode(String label) {
        this.label = label;
    }

    /** Human-readable name, used by the rail's theme control. */
    public String label() {
        return label;
    }

    /** Never returns null: a corrupt stored value falls back to LIGHT. */
    public static ThemeMode fromName(String name) {
        if (name != null) {
            for (ThemeMode m : values()) {
                if (m.name().equalsIgnoreCase(name)) {
                    return m;
                }
            }
        }
        return LIGHT;
    }
}
