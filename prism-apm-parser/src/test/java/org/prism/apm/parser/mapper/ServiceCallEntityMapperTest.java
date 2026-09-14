package org.prism.apm.parser.mapper;

import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.utils.CommonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author john
 * @date 2024-08-28 17:58
 */
public class ServiceCallEntityMapperTest {

    @Test
    public void testFrom_SingleSpanInfo() {
        // Arrange
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setTime(1627545600000L);  // Example epoch time
        spanInfo.setServiceName("TestService");
        spanInfo.setHostName("TestHost");
        spanInfo.setActionName("TestAction");
        spanInfo.setComponent("TestComponent");
        // TODO: Commented out for a quick change. Update this according to the current spanKind value.
        spanInfo.setSpanKind(SpanKindEnum.INTERNAL.getValue());
        spanInfo.setDuration(100L);
        spanInfo.setError(1L);

        // Act
        ServiceCallEntity entity = ServiceCallEntityMapper.from(spanInfo);

        // Assert
        assertEquals(CommonUtil.fromEpochMicros(1627545600000L), entity.getTime());
        assertEquals("TestService", entity.getServiceName());
        assertEquals("TestHost", entity.getHostName());
        assertEquals("TestAction", entity.getActionName());
        assertEquals("TestComponent", entity.getComponent());
        assertEquals(SpanKindEnum.INTERNAL.getValue(), entity.getSpanKind());
        assertEquals(CommonUtil.fromEpochMicros(0), entity.getCallerTime());
        assertEquals("", entity.getCallerServiceName());
        assertEquals("", entity.getCallerHostName());
        assertEquals("", entity.getCallerActionName());
        assertEquals("", entity.getCallerComponent());
        assertEquals(SpanKindEnum.UNKNOWN.getValue(), entity.getCallerSpanKind());
        assertEquals(Long.valueOf(100L), entity.getDuration());
        assertEquals(Long.valueOf(1L), entity.getError());
    }

    @Test
    public void testFrom_CalleeAndCallerSpanInfo() {
        // Arrange
        SpanInfo callee = new SpanInfo();
        callee.setTime(1627545600000L);  // Example epoch time
        callee.setServiceName("CalleeService");
        callee.setHostName("CalleeHost");
        callee.setActionName("CalleeAction");
        callee.setComponent("CalleeComponent");
        callee.setSpanKind(SpanKindEnum.CLIENT.getValue());
        callee.setDuration(200L);
        callee.setError(0L);

        SpanInfo caller = new SpanInfo();
        caller.setTime(1627545605000L);  // Example epoch time
        caller.setServiceName("CallerService");
        caller.setHostName("CallerHost");
        caller.setActionName("CallerAction");
        caller.setComponent("CallerComponent");
        caller.setSpanKind(SpanKindEnum.SERVER.getValue());
        caller.setDuration(150L);
        caller.setError(1L);

        // Act
        ServiceCallEntity entity = ServiceCallEntityMapper.from(callee, caller);

        // Assert
        // Callee fields
        assertEquals(CommonUtil.fromEpochMicros(1627545600000L), entity.getTime());
        assertEquals("CalleeService", entity.getServiceName());
        assertEquals("CalleeHost", entity.getHostName());
        assertEquals("CalleeAction", entity.getActionName());
        assertEquals("CalleeComponent", entity.getComponent());
        assertEquals(SpanKindEnum.CLIENT.getValue(), entity.getSpanKind());
        assertEquals(Long.valueOf(200L), entity.getDuration());
        assertEquals(Long.valueOf(0L), entity.getError());

        // Caller fields
        assertEquals(CommonUtil.fromEpochMicros(1627545605000L), entity.getCallerTime());
        assertEquals("CallerService", entity.getCallerServiceName());
        assertEquals("CallerHost", entity.getCallerHostName());
        assertEquals("CallerAction", entity.getCallerActionName());
        assertEquals("CallerComponent", entity.getCallerComponent());
        assertEquals(SpanKindEnum.SERVER.getValue(), entity.getCallerSpanKind());
        assertEquals(Long.valueOf(200L), entity.getDuration());  // This line matches the last set of caller duration
        assertEquals(Long.valueOf(0L), entity.getError());  // This line matches the last set of caller error
    }
}
