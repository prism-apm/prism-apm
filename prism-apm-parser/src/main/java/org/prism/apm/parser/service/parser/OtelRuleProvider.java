package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.OtelParserConfigLoader;
import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelParserConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OtelRuleProvider {

    private final OtelParserConfig config;
    private final OtelValueExtractor valueExtractor;

    public OtelRuleProvider(OtelParserConfigLoader loader, OtelValueExtractor valueExtractor) {
        this.config = loader.getOtelParserConfig();
        this.valueExtractor = valueExtractor;
    }

    /**
     * Finds the effective rule for a given span.
     * It first tries to find a specific component rule that matches the span.
     * If no specific rule is found, it returns the global default rule.
     * The component rules are pre-processed at load time to include defaults, so no runtime merging is needed.
     * @return The single, effective OtelComponentRule for this span.
     */
    public OtelComponentRule findEffectiveRuleForSpan(Map<String, Object> spanMap, Map<String, Object> scopeMap, Map<String, Object> resourceAttributes) {
        if (config == null || config.getParser() == null) {
            return null;
        }

        // Guard clause: If there are no specific rules to check, fall back to defaults immediately.
        if (config.getParser().getComponentDefinitions() == null) {
            return config.getParser().getDefaults();
        }

        // Try to find a specific, matching rule
        for (OtelComponentRule rule : config.getParser().getComponentDefinitions()) {
            for (Map<String, String> condition : rule.getMatch()) {
                for (Map.Entry<String, String> entry : condition.entrySet()) {
                    String keyPath = entry.getKey();
                    String regex = entry.getValue();
                    String actualValue = valueExtractor.resolvePath(keyPath, spanMap, scopeMap, resourceAttributes);

                    if (actualValue != null && actualValue.matches(regex)) {
                        return rule; // Found a match, return immediately.
                    }
                }
            }
        }

        // If no specific rule was found, return the global default rule
        return config.getParser().getDefaults();
    }
}
