package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.constants.SpanInfoFields;
import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.init.bo.SpanFieldReplacer;
import org.prism.apm.parser.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class OpenTelemetrySpanInfoBuilder {

    private final OtelRuleProvider ruleProvider;
    private final OtelValueExtractor valueExtractor;
    private SpanFieldReplacer replacer;

    public OpenTelemetrySpanInfoBuilder(OtelRuleProvider ruleProvider, OtelValueExtractor valueExtractor,
                                        SpanFieldReplacer replacer) {
        this.ruleProvider = ruleProvider;
        this.valueExtractor = valueExtractor;
        this.replacer = replacer;
    }

    public SpanInfo build(Map<String, Object> spanMap, Map<String, Object> scopeMap, Map<String, Object> resourceAttributes) {
        SpanInfo spanInfo = new SpanInfo();
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        if (effectiveRule == null) {
            log.warn("No matching rule found for span: {}", spanMap);
            return null;
        }

        // Call individual getter methods
        spanInfo.setServiceName(getServiceName(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setHostName(getHostName(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setSpanKind(getSpanKind(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setActionName(getActionName(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setTargetName(getTargetName(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setComponent(getComponent(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setSystemType(getSystemType(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setError(getError(effectiveRule, spanMap, scopeMap, resourceAttributes));
        spanInfo.setRemoteAddr(getRemoteAddr(effectiveRule, spanMap, scopeMap, resourceAttributes));

        // Set non-configurable fields
        spanInfo.setTraceId((String) spanMap.get("traceId"));
        spanInfo.setSpanId((String) spanMap.get("spanId"));
        spanInfo.setParentSpanId((String) spanMap.get("parentSpanId"));

        long startTimeNano = Long.parseLong((String) spanMap.get("startTimeUnixNano"));
        long endTimeNano = Long.parseLong((String) spanMap.get("endTimeUnixNano"));
        spanInfo.setDuration(TimeUnit.NANOSECONDS.toMicros(endTimeNano - startTimeNano));
        spanInfo.setTime(TimeUnit.NANOSECONDS.toMicros(startTimeNano));
        spanInfo.setServerArrivalTime(System.currentTimeMillis());

        return spanInfo;
    }

    String getServiceName(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        return valueExtractor.getValue(rule, SpanInfoFields.SERVICE_NAME, span, scope, resource);
    }

    String getHostName(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        return valueExtractor.getValue(rule, SpanInfoFields.HOST_NAME, span, scope, resource);
    }

    Short getSpanKind(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        return Short.valueOf(valueExtractor.getValue(rule, SpanInfoFields.SPAN_KIND, span, scope, resource));
    }

    String getActionName(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {

        String originalValue = valueExtractor.getValue(rule, SpanInfoFields.ACTION_NAME, span, scope, resource);
        String newValue = replacer.match(
                getServiceName(rule, span, scope, resource),
                getComponent(rule, span, scope, resource),
                SpanInfoFields.ACTION_NAME,
                originalValue
        );

        if (newValue != null) {
            return newValue;
        }

        return originalValue;
    }

    String getTargetName(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        String originalValue = valueExtractor.getValue(rule, SpanInfoFields.TARGET_NAME, span, scope, resource);
        String newValue = replacer.match(
                getServiceName(rule, span, scope, resource),
                getComponent(rule, span, scope, resource),
                SpanInfoFields.TARGET_NAME,
                originalValue
        );

        String rtnValue = originalValue;
        if (newValue != null) {
            rtnValue = newValue;
        }

        return StringUtils.removeNullCharacters(rtnValue);

    }

    String getComponent(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        return valueExtractor.getValue(rule, SpanInfoFields.COMPONENT, span, scope, resource);
    }

    String getSystemType(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        return valueExtractor.getValue(rule, SpanInfoFields.SYSTEM_TYPE, span, scope, resource);
    }

    Long getError(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        String errorVal = valueExtractor.getValue(rule, SpanInfoFields.ERROR, span, scope, resource);
        return errorVal != null ? Long.parseLong(errorVal) : 0L;
    }

    String getRemoteAddr(OtelComponentRule rule, Map<String, Object> span, Map<String, Object> scope, Map<String, Object> resource) {
        Short kind = getSpanKind(rule, span, scope, resource);
        if (kind == null) return null;

        Map<String, Object> attributes = (Map<String, Object>) span.get("attributes");
        if (attributes == null) return "";

        String remoteAddr = null;
        if (Objects.equals(SpanKindEnum.SERVER.getValue(), kind)) {
            remoteAddr = (String) attributes.get("client.address");
        } else if (Objects.equals(SpanKindEnum.CLIENT.getValue(), kind)) {
            remoteAddr = (String) attributes.get("server.address");
        }

        if (remoteAddr == null) {
            remoteAddr = (String) attributes.get("network.peer.address");
        }

        String newValue = replacer.match(
                getServiceName(rule, span, scope, resource),
                getComponent(rule, span, scope, resource),
                SpanInfoFields.REMOTE_ADDR,
                remoteAddr
        );

        if (newValue != null) {
            remoteAddr = newValue;
        }
        
        return remoteAddr == null ? "" : remoteAddr;
    }
}
