package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelOutputRule;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OtelValueExtractor {

    public String getValue(OtelComponentRule effectiveRule, String fieldName, Map<String, Object> spanMap, Map<String, Object> scopeMap, Map<String, Object> resourceAttributes) {
        if (effectiveRule == null || effectiveRule.getOutputs() == null) {
            return null;
        }

        OtelOutputRule outputRule = effectiveRule.getOutputs().get(fieldName);
        if (outputRule == null) {
            return null;
        }

        if (outputRule.getVals() == null) {
            return outputRule.getDefaultVal();
        }

        for (String path : outputRule.getVals()) {
            String value = resolvePath(path, spanMap, scopeMap, resourceAttributes);
            if (value != null) {
                return value;
            }
        }

        return outputRule.getDefaultVal();
    }

    public String resolvePath(String path, Map<String, Object> spanMap, Map<String, Object> scopeMap, Map<String, Object> resourceAttributes) {
        if (path == null) return null;

        String[] parts = path.split("__");
        if (parts.length < 2) { // Path must have at least a source and a final key.
            return null;
        }

        // 1. Determine the starting map based on the first part.
        Map<String, Object> currentMap;
        switch (parts[0]) {
            case "span-attributes":
                currentMap = (Map<String, Object>) spanMap.get("attributes");
                break;
            case "resource-attributes":
                currentMap = resourceAttributes;
                break;
            case "spans-scope":
                currentMap = scopeMap;
                break;
            case "span":
                currentMap = spanMap;
                break;
            default:
                // todo print error
                return null;
        }

        if (currentMap == null) return null;

        // 2. Traverse the nested maps for intermediate parts.
        for (int i = 1; i < parts.length - 1; i++) {
            Object nextLevel = currentMap.get(parts[i]);
            if (nextLevel instanceof Map) {
                currentMap = (Map<String, Object>) nextLevel;
            } else {
                return null;
            }
        }

        // 3. Use the last part as the final key to get the value.
        String finalKey = parts[parts.length - 1];
        Object finalValue = currentMap.get(finalKey);

        return finalValue != null ? String.valueOf(finalValue) : null;
    }
}
