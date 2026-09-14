package org.prism.apm.parser.mapper;

import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.utils.CommonUtil;

import java.time.OffsetDateTime;

import static org.prism.apm.parser.utils.CommonUtil.getCurrentTimeOffsetDateTime;

/**
 * @author john
 * @date 2024-08-26 11:14
 */
public class ServiceCallEntityMapper {

    public static final String TEXT_DEFAULT_VALUE = "";
    public static final OffsetDateTime DATE_DEFAULT_VALUE = CommonUtil.fromEpochMicros(0);

    public static ServiceCallEntity from(SpanInfo spanInfo) {

        ServiceCallEntity entity = new ServiceCallEntity();

        entity.setTime(CommonUtil.fromEpochMicros(spanInfo.getTime()));
        entity.setTraceId(spanInfo.getTraceId());
        entity.setSpanId(spanInfo.getSpanId());
        entity.setServiceName(spanInfo.getServiceName());
        entity.setHostName(spanInfo.getHostName());
        entity.setActionName(spanInfo.getActionName());
        entity.setTargetName(spanInfo.getTargetName());
        entity.setSystemType(spanInfo.getSystemType());
        entity.setRemoteAddr(spanInfo.getRemoteAddr());
        entity.setComponent(spanInfo.getComponent());
        entity.setSpanKind(spanInfo.getSpanKind());
        // 1970-01-01T00:00Z
        entity.setCallerTime(DATE_DEFAULT_VALUE);
        entity.setCallerSpanId(TEXT_DEFAULT_VALUE);
        entity.setCallerServiceName(TEXT_DEFAULT_VALUE);
        entity.setCallerHostName(TEXT_DEFAULT_VALUE);
        entity.setCallerActionName(TEXT_DEFAULT_VALUE);
        entity.setCallerTargetName(TEXT_DEFAULT_VALUE);
        entity.setCallerSystemType(TEXT_DEFAULT_VALUE);
        entity.setCallerRemoteAddr(TEXT_DEFAULT_VALUE);
        entity.setCallerComponent(TEXT_DEFAULT_VALUE);
        // todo Previously：entity.setCallerSpanKind(TEXT_DEFAULT_VALUE);
        entity.setCallerSpanKind((short)0);
        entity.setCnt(1L);
        entity.setDuration(spanInfo.getDuration());
        entity.setError(spanInfo.getError());
        OffsetDateTime currentTimeOffsetDateTime = getCurrentTimeOffsetDateTime();
        entity.setCreateTime(currentTimeOffsetDateTime);
        entity.setUpdateTime(currentTimeOffsetDateTime);

        return entity;

    }

    public static ServiceCallEntity from(SpanInfo callee, SpanInfo caller) {

        ServiceCallEntity entity = new ServiceCallEntity();
        // Set callee properties whose names match entity property names. Copy callee into entity.

        entity.setTime(CommonUtil.fromEpochMicros(callee.getTime()));
        entity.setTraceId(callee.getTraceId());
        entity.setSpanId(callee.getSpanId());
        entity.setServiceName(callee.getServiceName());
        entity.setHostName(callee.getHostName());
        entity.setActionName(callee.getActionName());
        entity.setTargetName(callee.getTargetName());
        entity.setSystemType(callee.getSystemType());
        entity.setRemoteAddr(callee.getRemoteAddr());
        entity.setComponent(callee.getComponent());
        entity.setSpanKind(callee.getSpanKind());
        entity.setCallerTime(CommonUtil.fromEpochMicros(caller.getTime()));
        entity.setCallerSpanId(caller.getSpanId());
        entity.setCallerServiceName(caller.getServiceName());
        entity.setCallerHostName(caller.getHostName());
        entity.setCallerActionName(caller.getActionName());
        entity.setCallerTargetName(caller.getTargetName());
        entity.setCallerSystemType(caller.getSystemType());
        entity.setCallerRemoteAddr(caller.getRemoteAddr());
        entity.setCallerComponent(caller.getComponent());
        entity.setCallerSpanKind(caller.getSpanKind());
        entity.setCnt(1L);
        entity.setDuration(callee.getDuration());
        entity.setError(callee.getError());
        OffsetDateTime currentTimeOffsetDateTime = getCurrentTimeOffsetDateTime();
        entity.setCreateTime(currentTimeOffsetDateTime);
        entity.setUpdateTime(currentTimeOffsetDateTime);

        return entity;

    }

}
