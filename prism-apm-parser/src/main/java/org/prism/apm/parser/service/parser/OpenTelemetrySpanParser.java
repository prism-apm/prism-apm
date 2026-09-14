package org.prism.apm.parser.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.prism.apm.parser.model.otel.OtelResourceSpan;
import org.prism.apm.parser.model.otel.OtelScopeSpan;
import org.prism.apm.parser.model.otel.OtelTraceData;
import org.prism.apm.parser.model.span.SpanInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "parser.type", havingValue = "opentelemetry", matchIfMissing = true)
@Slf4j
public class OpenTelemetrySpanParser implements ISpanParser {

    private final OpenTelemetryTraceDeserializer deserializer;
    private final OpenTelemetrySpanInfoBuilder spanInfoBuilder;

    public OpenTelemetrySpanParser(OpenTelemetryTraceDeserializer deserializer, OpenTelemetrySpanInfoBuilder spanInfoBuilder) {
        this.deserializer = deserializer;
        this.spanInfoBuilder = spanInfoBuilder;
    }

    @Override
    public List<SpanInfo> parse(String trace) {
        OtelTraceData traceData;
        try {
            traceData = deserializer.deserialize(trace);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OpenTelemetry JSON trace. trace:{}", trace, e);
            return Collections.emptyList();
        }

        if (traceData == null) {
            return Collections.emptyList();
        }

        List<SpanInfo> spanInfos = new ArrayList<>();
        for (OtelResourceSpan rs : traceData.getResourceSpans()) {
            Map<String, Object> resourceAttributes = rs.getResource() != null ? rs.getResource().getAttributes() : Collections.emptyMap();
            for (OtelScopeSpan ss : rs.getScopeSpans()) {
                Map<String, Object> scopeMap = ss.getScope();
                for (Map<String, Object> spanMap : ss.getSpans()) {
                    // only drop unexpected/illegal span
                    try {
                        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);
                        if (spanInfo == null) {
                            log.error("Failed to build SpanInfo from span map: {}. trace:{}", spanMap, trace);
                        }
                        else if (spanInfo.checkRequiredFields()) {
                            log.debug("SpanInfo is not complete. spanInfo:{}, trace:{}", spanInfo, trace);
                        }
                        else {
                            spanInfos.add(spanInfo);
                        }
                    } catch (Exception e) {
                        log.error("Failed to build SpanInfo from span map: {}. trace:{}", spanMap, trace, e);
                    }
                }
            }
        }
        return spanInfos;
    }
}
