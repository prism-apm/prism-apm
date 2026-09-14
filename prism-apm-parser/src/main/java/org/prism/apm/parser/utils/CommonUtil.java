package org.prism.apm.parser.utils;


import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static java.lang.System.currentTimeMillis;

/**
 * @author peter
 * @create 2019-07-01
 */
public class CommonUtil {

    public static OffsetDateTime fromEpochMicros(long microsecondsSinceEpoch) {

        // 1. Calculate seconds from microseconds
        long seconds = microsecondsSinceEpoch / 1_000_000L;

        // 2. Calculate the nanosecond part within the current second
        // First get the remaining microseconds, then multiply by 1000 to get nanoseconds
        long microsOfSecond = microsecondsSinceEpoch % 1_000_000L;
        long nanosOfSecond = microsOfSecond * 1_000L;

        // 3. Create an Instant from seconds and nanoseconds
        Instant instant = Instant.ofEpochSecond(seconds, nanosOfSecond);

        // 4. Set the time zone to UTC and convert to OffsetDateTime
        ZoneOffset offset = ZoneOffset.UTC;
        return OffsetDateTime.ofInstant(instant, offset);
    }


    public static OffsetDateTime withStandardPrecision(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Formats a timestamp of milliseconds since the Unix epoch into a string.
     * This method is thread-safe.
     *
     * @param epochMilli The number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
     * @param formatPattern The format pattern, e.g., "yyyy-MM-dd HH:mm:ss".
     * @return The formatted date-time string.
     */
    public static String formatEpochMilli(long epochMilli, String formatPattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        return OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC).format(formatter);
    }


    public static long getCurrentTimeMillis() {
        return currentTimeMillis();
    }


    public static OffsetDateTime getCurrentTimeOffsetDateTime() {
        return Instant.ofEpochMilli(currentTimeMillis()).atOffset(ZoneOffset.UTC);

    }

    public static Instant fromEpochMicroToInstant(long microsSinceEpoch) {
        long seconds = microsSinceEpoch / 1_000_000;
        long remainingMicros = microsSinceEpoch % 1_000_000;
        long remainingNanos = remainingMicros * 1_000;

        return Instant.ofEpochSecond(seconds, remainingNanos);
    }

}


