package org.prism.apm.ui.server.utils;

import org.prism.apm.ui.server.entity.dto.route.RouteInfo;
import org.prism.apm.ui.server.entity.model.service.ServiceCallView;
import org.prism.apm.ui.server.entity.param.common.DateParam;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FillDataHoleUtil {

    //Minimum step is 1 minute
    public static final long MIN_DATA_STEP_UNIT = Duration.ofMinutes(1L).toMillis();

    public static List<ServiceCallView> fillServiceMinuteCallDataHoleInDatetimeRange(DateParam dateParam, List<ServiceCallView> list, RouteInfo routeInfo) {
        ZoneId zoneId = ZoneId.of(dateParam.getTimezone());
        long start = DateUtils.convertDatetime2Timestamp(dateParam.getStartDate(), zoneId).getTime();
        long end = DateUtils.convertDatetime2Timestamp(dateParam.getEndDate(), zoneId).getTime();

        return fillHolesGeneric(
                start,
                end,
                list,
                routeInfo.getDataStep() * MIN_DATA_STEP_UNIT,

                x -> x.getTimeb()
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli(),

                millis -> {
                    ServiceCallView tmp = ServiceCallView.createInstanceWithDefaultValue();

                    // Convert timestamp back to LocalDateTime
                    tmp.setTimeb(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId));

                    return tmp;
                }
        );
    }

    /**
     * Generic time axis filling method
     *
     * @param startTs       Start timestamp (ms)
     * @param endTs         End timestamp (ms)
     * @param sourceList    Source data list
     * @param step          Step (ms)
     * @param timeExtractor Function that extracts timestamp from an object, for example x -> x.getTimeb().getTime()
     * @param objectFactory Function that creates a default object from a timestamp, for example t -> new Obj(t)
     * @param <T>           Data object type
     * @return Filled list
     */
    public static <T> List<T> fillHolesGeneric(long startTs,
                                               long endTs,
                                               List<T> sourceList,
                                               long step,
                                               Function<T, Long> timeExtractor,
                                               Function<Long, T> objectFactory) {

        Map<Long, T> map = sourceList.stream()
                .collect(Collectors.toMap(timeExtractor, Function.identity(), (x1, x2) -> x1));

        // =========================================================
        // [Core fix] Calculate the aligned start time
        // =========================================================
        // Example: step=5 minutes (300000 ms), startTs=10:03
        // remainder = 3 minutes
        // alignedStart = 10:03 - 3 minutes = 10:00, so it can hit data at 10:00 and 10:05
        long remainder = startTs % step;

        // If you want to strictly stay within the query range, use ceiling
        long alignedStart = (remainder == 0) ? startTs : (startTs + step - remainder);

        // 2. Start the loop from the aligned time
        List<T> resultList = new ArrayList<>();
        for (long current = alignedStart; current <= endTs; current += step) {

            T item = map.get(current);

            if (item == null) {
                item = objectFactory.apply(current);
            }
            resultList.add(item);
        }

        return resultList;
    }
}
