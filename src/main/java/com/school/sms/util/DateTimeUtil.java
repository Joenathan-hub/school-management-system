package com.school.sms.util;

import java.time.format.DateTimeFormatter;

/**
 * Single source of truth for date/time formatting across the app, so every
 * screen, receipt, and printed list shows dates and times the same way:
 * day/month/year, and time in 24-hour (HH:mm) format.
 */
public class DateTimeUtil {
    public static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm"); // 24-hour clock
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateTimeUtil() {}
}
