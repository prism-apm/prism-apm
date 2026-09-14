
package org.prism.apm.ui.server.constant;

import com.google.common.collect.Lists;

import java.util.List;

public class SpanConstants {

    //== field names
    public static final String AVG_DURATION = "avg_duration";
    public static final String MAX_DURATION = "max_duration";
    public static final String MIN_DURATION = "min_duration";
    public static final String P90_DURATION = "p90_duration";
    public static final String P95_DURATION = "p95_duration";
    public static final String P99_DURATION = "p99_duration";
    public static final String CNT = "cnt";
    public static final String ERROR_NUM = "error_num";

    //== field names collections
    public static final List<String> DURATION_INDICATORS = Lists.newArrayList(
        AVG_DURATION, MAX_DURATION, MIN_DURATION,
        P90_DURATION, P95_DURATION, P99_DURATION
    );

    //== field default values
    public static final String DEFAULT_STRING_VALUE_WHEN_NO_DATA = "";
    public static final String DEFAULT_DISPLAY_VALUE_WHEN_NO_DATA = "-";
    public static final Short DEFAULT_SHORT_VALUE_WHEN_NO_DATA = 0;
    public static final String DEFAULT_UUID_VALUE_WHEN_NO_DATA = "00000000-0000-0000-0000-000000000000";

}
