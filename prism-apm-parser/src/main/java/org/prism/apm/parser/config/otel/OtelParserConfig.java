package org.prism.apm.parser.config.otel;

import lombok.Data;

/**
 * Root class for mapping the parser-component-config.yml file.
 */
@Data
public class OtelParserConfig {
    private String version;
    private OtelParserDefinition parser;
}
