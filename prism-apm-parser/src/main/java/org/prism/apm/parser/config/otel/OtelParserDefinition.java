package org.prism.apm.parser.config.otel;

import lombok.Data;
import java.util.List;

@Data
public class OtelParserDefinition {
    private List<OtelComponentRule> componentDefinitions;
    private OtelComponentRule defaults;
}
