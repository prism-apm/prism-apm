package org.prism.apm.parser.model.otel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OtelResourceSpan {

    @JsonProperty("resource")
    private OtelResource resource;

    @JsonProperty("scopeSpans")
    private List<OtelScopeSpan> scopeSpans;

    @JsonProperty("schemaUrl")
    private String schemaUrl;
}
