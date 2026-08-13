package com.school.sms.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Generates Student and Worker IDs in the format:
 *   [S|W]-[SCHOOL_INITIALS][YEAR][MONTH]-[4-digit random]
 *   e.g. S-SHS2026 07-4821
 *
 * The S-/W- prefix (suggestion #6) disambiguates student vs worker IDs at a
 * glance, since otherwise the two ID schemes look identical.
 *
 * Because the 4-digit suffix is random, collisions are possible. Pass an
 * `existsCheck` predicate (typically backed by a DB lookup) and this class
 * will regenerate until it finds an ID that isn't already taken.
 */
public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Type {
        STUDENT("S"),
        WORKER("W");

        final String prefix;
        Type(String prefix) { this.prefix = prefix; }
    }

    /**
     * @param type          STUDENT or WORKER
     * @param schoolInitials configured once in system settings, e.g. "SHS"
     * @param joinDate      date of admission / joining
     * @param existsCheck   returns true if the candidate ID is already in the database
     */
    public static String generate(Type type, String schoolInitials, LocalDate joinDate, Predicate<String> existsCheck) {
        String year = String.valueOf(joinDate.getYear());
        String month = String.format("%02d", joinDate.getMonthValue());

        String candidate;
        int attempts = 0;
        do {
            String randomDigits = String.format("%04d", RANDOM.nextInt(10000));
            candidate = String.format("%s-%s%s%s-%s", type.prefix, schoolInitials, year, month, randomDigits);
            attempts++;
            if (attempts > 50) {
                throw new IllegalStateException("Could not generate a unique ID after 50 attempts. " +
                        "Check the existsCheck logic or ID space exhaustion.");
            }
        } while (existsCheck.test(candidate));

        return candidate;
    }
}
