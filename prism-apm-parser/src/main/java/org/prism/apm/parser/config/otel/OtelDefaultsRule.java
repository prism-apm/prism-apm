package org.prism.apm.parser.config.otel;

import lombok.Data;
import java.util.Map;

@Data
public class OtelDefaultsRule {
    private Map<String, OtelOutputRule> outputs;
}
