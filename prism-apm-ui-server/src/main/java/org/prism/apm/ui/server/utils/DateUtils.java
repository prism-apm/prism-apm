package org.prism.apm.ui.server.utils;

import org.prism.apm.ui.server.exceptions.BizException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateUtils {
    private static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);

    public static long getTimeBetweenInMinutes(String start, String end) {
        try {
            LocalDateTime startDate = parseDateTimeString(start);
            LocalDateTime endDate = parseDateTimeString(end);

            // Duration is dedicated to calculating time differences
            Duration duration = Duration.between(startDate, endDate);

            // Get the minute difference directly
            return duration.toMinutes();
        } catch (DateTimeParseException e) {
            // Throw a more standard exception or a custom exception
            throw new IllegalArgumentException("Invalid date format, cannot parse: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate the day interval between two times through date objects
     */
    public static long getTimeBetweenInDays(String start, String end) {
        try {
            // 1. Use the predefined formatter to parse strings into LocalDateTime objects
            LocalDateTime startDate = parseDateTimeString(start);
            LocalDateTime endDate = parseDateTimeString(end);

            // 2. Use Duration.between to calculate the exact time difference between two time points and get a Duration object
            Duration duration = Duration.between(startDate, endDate);

            return duration.toDays();

        } catch (DateTimeParseException e) {
            // Catch parsing exceptions and wrap them in a more general IllegalArgumentException,
            // clearly telling callers that the parameter format is invalid.
            throw new BizException("Invalid date format. Use 'yyyy-MM-dd HH:mm:ss' format");
        }
    }

    public static Timestamp convertDatetime2Timestamp(String datetime, ZoneId zoneId) {
        try {
            LocalDateTime localDateTime = parseDateTimeString(datetime);
            return Timestamp.from(localDateTime.atZone(zoneId).toInstant());
        } catch (DateTimeParseException e) {
            throw new BizException("Invalid date format");
        }
    }

    public static String getDefaultTimeBucket() {
        return "1 min";
    }

    public static String formatDateTime(Date date) {
        return DATE_TIME_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    public static LocalDateTime parseDateTimeString(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }
}
