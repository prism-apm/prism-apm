package org.prism.apm.parser.model.otel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.prism.apm.parser.model.otel.deserializer.OtelSpanMapDeserializer;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OtelScopeSpan {

    @JsonProperty("scope")
    private Map<String, Object> scope;

    @JsonProperty("spans")
    @JsonDeserialize(contentUsing = OtelSpanMapDeserializer.class)
    private List<Map<String, Object>> spans;
}
