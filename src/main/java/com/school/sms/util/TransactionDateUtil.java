package com.school.sms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Converts a user-picked LocalDate into a full timestamp for a transaction.
 *
 * If the user picks today, we keep the real current time — that's accurate.
 * If they backdate, the exact time of day is unknowable, so we use noon as a
 * neutral marker rather than inventing a precise-looking time. The true
 * "when was this entered" is always preserved separately in recordedAt.
 */
public class TransactionDateUtil {

    public static LocalDateTime toTransactionDateTime(LocalDate picked) {
        if (picked == null) return LocalDateTime.now();
        if (picked.equals(LocalDate.now())) return LocalDateTime.now();
        return picked.atTime(LocalTime.NOON);
    }

    /** When correcting an existing record, keep its original time-of-day and change only the date. */
    public static LocalDateTime replaceDateKeepTime(LocalDateTime original, LocalDate newDate) {
        if (newDate == null) return original;
        if (original == null) return newDate.atTime(LocalTime.NOON);
        return newDate.atTime(original.toLocalTime());
    }

    private TransactionDateUtil() {}
}