package org.prism.apm.ui.server.utils;

import org.prism.apm.ui.server.route.TableRoutingRules;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TimeAxisUtils {

    /**
     * Calculate time axis labels
     * Generate formatted time axis label text from the input timestamp list and date step
     *
     * @param timestamps Timestamp list containing time points to format
     * @param dateStep Date step used to decide the time axis label display format
     * @param zoneId User time zone used to convert epoch milliseconds to local time strings
     * @return Formatted time axis label string list
     */
    public static List<String> calculateTimeAxis(List<Timestamp> timestamps, long dateStep, ZoneId zoneId) {
        // 1. Null or empty check
        if (timestamps == null || timestamps.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Extract all distinct date parts
        Set<LocalDate> dateSet = timestamps.stream()
                .map(ts -> Instant.ofEpochMilli(ts.getTime()).atZone(zoneId).toLocalDate())
                .collect(Collectors.toSet());

        // 3. Dynamically determine the time format based on date span and step
        DateTimeFormatter formatter;

        if (dateSet.size() <= 1) {
            // If there is only one day, or no data
            formatter = DateTimeFormatter.ofPattern("HH:mm");
        } else {
            // If it spans days, check the step
            if (dateStep >= TableRoutingRules.TableRoutingStep.ONE_DAY.getStepDuration().toMillis()) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            } else {
                formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            }
        }

        // 4. Convert and return the result list
        return timestamps.stream()
                .map(ts -> Instant.ofEpochMilli(ts.getTime()).atZone(zoneId).format(formatter))
                .collect(Collectors.toList());
    }

}
