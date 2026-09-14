package org.prism.apm.parser.config.otel;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OtelComponentRule {
    private String name;
    private List<Map<String, String>> match;
    private Map<String, OtelOutputRule> outputs;
}
