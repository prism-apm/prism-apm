package org.prism.apm.parser.config.otel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OtelOutputRule {
    private List<String> vals;
    @JsonProperty("default")
    private String defaultVal;
}
