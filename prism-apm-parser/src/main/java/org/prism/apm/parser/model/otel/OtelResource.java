package org.prism.apm.parser.model.otel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.prism.apm.parser.model.otel.deserializer.OtelAttributesDeserializer;
import lombok.Data;

import java.util.Map;

@Data
public class OtelResource {

    @JsonDeserialize(using = OtelAttributesDeserializer.class)
    private Map<String, Object> attributes;
}
