package org.prism.apm.ui.server.entity.param.common;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class DateParam {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String FIELD_NAME_START_DATE = "startDate";
    public static final String FIELD_NAME_END_DATE = "endDate";
    public static final String FIELD_NAME_TIMEZONE = "timezone";

    private String startDate;
    private String endDate;
    private String timezone;

    private String startUTC;
    private String endUTC;

    public void convertToUTC() {
        // 1. Parse time zone
        ZoneId userZone = ZoneId.of(this.timezone); // "Asia/Tokyo"

        // 2. Parse local time (wall-clock time)
        LocalDateTime startLdt = LocalDateTime.parse(startDate, FMT);
        LocalDateTime endLdt = LocalDateTime.parse(endDate, FMT);

        // 3. Add time zone information (ZonedDateTime)
        // This step handles daylight saving time: Java automatically calculates the offset from UTC for that date and moment
        ZonedDateTime startZdt = startLdt.atZone(userZone);
        ZonedDateTime endZdt = endLdt.atZone(userZone);

        // 4. Convert to UTC Instant for SQL
        Instant startUtc = startZdt.toInstant();
        Instant endUtc = endZdt.toInstant();

        this.startUTC = startUtc.toString();
        this.endUTC = endUtc.toString();
    }
}
