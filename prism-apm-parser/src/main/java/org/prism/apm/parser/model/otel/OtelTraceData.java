package org.prism.apm.parser.model.otel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * Represents the root object of an OTLP/JSON payload.
 */
@Data
public class OtelTraceData {

    @NonNull
    @JsonProperty("resourceSpans")
    private List<OtelResourceSpan> resourceSpans;

    @JsonCreator
    public OtelTraceData(
            @JsonProperty(value = "resourceSpans", required = true) List<OtelResourceSpan> resourceSpans
    ) {
        this.resourceSpans = resourceSpans;
    }
}
