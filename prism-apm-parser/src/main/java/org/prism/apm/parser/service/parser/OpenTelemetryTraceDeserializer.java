package org.prism.apm.parser.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prism.apm.parser.model.otel.OtelTraceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deserializes an OpenTelemetry JSON trace into custom OtelTraceData POJOs.
 * The actual deserialization logic is handled by annotations on the model classes.
 */
@Component
@Slf4j
public class OpenTelemetryTraceDeserializer {

    private final ObjectMapper objectMapper;


    public OpenTelemetryTraceDeserializer() {
        // todo Should this Jackson configuration use JacksonConfig?
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Deserializes the JSON string into a structured OtelTraceData object.
     *
     * @param trace The raw OTLP/JSON string.
     * @return A {@link OtelTraceData} object, or {@code null} if parsing fails.
     */
    public OtelTraceData deserialize(String trace) throws JsonProcessingException {
        if (trace == null) {
            return null;
        }

        return objectMapper.readValue(trace, OtelTraceData.class);
    }
}