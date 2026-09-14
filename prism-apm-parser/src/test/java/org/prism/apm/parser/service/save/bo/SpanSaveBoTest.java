package org.prism.apm.parser.service.save.bo;


import org.prism.apm.parser.model.span.SpanInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author john
 * @date 2024-08-28 17:26
 */
public class SpanSaveBoTest {

    private SpanSaveBo spanSaveBo;

    @BeforeEach
    public void setUp() {
        spanSaveBo = new SpanSaveBo();
    }


    @Test
    public void testIsTimeout_ShouldReturnTrue_WhenTimeoutExceeded() {
        long currTime = System.currentTimeMillis();
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setServerArrivalTime(currTime - TimeUnit.SECONDS.toMillis(100));

        boolean result = spanSaveBo.isTimeout(spanInfo, currTime, TimeUnit.SECONDS.toMillis(60));

        assertTrue(result);
    }

    @Test
    public void testIsTimeout_ShouldReturnFalse_WhenTimeoutNotExceeded() {
        long currTime = System.currentTimeMillis();
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setServerArrivalTime(currTime - TimeUnit.SECONDS.toMillis(30));

        boolean result = spanSaveBo.isTimeout(spanInfo, currTime, TimeUnit.SECONDS.toMillis(60));

        assertFalse(result);
    }

    @Test
    public void testIsTimeout_ShouldHandleExactThreshold() {
        long currTime = System.currentTimeMillis();
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setServerArrivalTime(currTime - TimeUnit.SECONDS.toMillis(60));

        boolean result = spanSaveBo.isTimeout(spanInfo, currTime, TimeUnit.SECONDS.toMillis(60));

        assertFalse(result);
    }

    @Test
    public void testIsTimeout_ShouldReturnFalse_WhenCurrentTimeEqualsServerArrivalTime() {
        long currTime = System.currentTimeMillis();
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setServerArrivalTime(currTime);

        boolean result = spanSaveBo.isTimeout(spanInfo, currTime, TimeUnit.SECONDS.toMillis(60));

        assertFalse(result);
    }

    @Test
    public void testIsTimeout_ShouldReturnTrue_WhenTimeoutJustExceeded() {
        long currTime = System.currentTimeMillis();
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setServerArrivalTime(currTime - TimeUnit.SECONDS.toMillis(61));

        boolean result = spanSaveBo.isTimeout(spanInfo, currTime, TimeUnit.SECONDS.toMillis(60));

        assertTrue(result);
    }

    @Test
    public void testIsNoCallerSpan_WhenParentSpanIdIsNotEmpty() {
        SpanInfo spanInfo = new SpanInfo();
//        spanInfo.setSpanKind(SpanKindEnum.SERVER.getName());
        spanInfo.setParentSpanId("parentId");

        boolean result = spanSaveBo.isNoCallerSpan(spanInfo);

        assertFalse(result);
    }

    @Test
    public void testIsNoCallerSpan_WhenParentSpanIdIsNull() {
        SpanInfo spanInfo = new SpanInfo();
//        spanInfo.setSpanKind(SpanKindEnum.SERVER.getName());
        spanInfo.setParentSpanId(null);

        boolean result = spanSaveBo.isNoCallerSpan(spanInfo);

        assertTrue(result);
    }


    @Test
    public void testIsNoCallerSpan_WhenParentSpanIdIsEmpty() {
        SpanInfo spanInfo = new SpanInfo();
//        spanInfo.setSpanKind(SpanKindEnum.SERVER.getName());
        spanInfo.setParentSpanId("");

        boolean result = spanSaveBo.isNoCallerSpan(spanInfo);

        assertTrue(result);
    }

}
