package org.prism.apm.ui.server.utils;

import java.math.BigDecimal;

public class UnitConvertUtils {

    public static BigDecimal convertMicroSecondsToMilliseconds_BigDecimal(BigDecimal microseconds) {
        if (microseconds == null) {
            return BigDecimal.ZERO;
        }

        // 1 millisecond = 1000 microseconds
        BigDecimal divisor = new BigDecimal("1000");

        // Perform exact division directly
        return microseconds.divide(divisor);
    }

    public static BigDecimal convertMicroSecondsToSeconds_BigDecimal(BigDecimal microseconds) {
        if (microseconds == null) {
            return BigDecimal.ZERO;
        }

        // 1 second = 1,000,000 microseconds
        BigDecimal divisor = new BigDecimal("1000000");

        // Perform exact division directly
        return microseconds.divide(divisor);
    }

    public static BigDecimal convertMillisecondsToMicroseconds_BigDecimal(BigDecimal milliseconds) {
        if (milliseconds == null) {
            return BigDecimal.ZERO;
        }

        // 1 millisecond = 1000 microseconds
        BigDecimal multiplicand = new BigDecimal("1000");

        return milliseconds.multiply(multiplicand);
    }

}
