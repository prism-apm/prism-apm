package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelOutputRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OtelValueExtractorTest {

    private OtelValueExtractor extractor;

    // Shared fields for test data
    private Map<String, Object> spanMap;
    private Map<String, Object> resourceAttributes;
    private Map<String, Object> scopeMap;

    @BeforeEach
    public void setUp() {
        // Only initialize the class under test
        extractor = new OtelValueExtractor();
    }

    /**
     * Prepares a default set of test data suitable for most resolvePath tests.
     */
    private void prepareResolvePathTestData() {
        resourceAttributes = new HashMap<>();
        resourceAttributes.put("service.name", "prism-apm-parser");
        resourceAttributes.put("telemetry.sdk.language", "java");

        scopeMap = new HashMap<>();
        scopeMap.put("name", "io.opentelemetry.jdbc");
        scopeMap.put("version", "1.0.0");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("db.system", "mysql");
        attributes.put("http.response.status_code", 200);

        spanMap = new HashMap<>();
        spanMap.put("name", "SELECT from users");
        spanMap.put("kind", "CLIENT");
        spanMap.put("attributes", attributes);
    }

    /**
     * Prepares test data suitable for getValue tests.
     * For now, it's the same as resolvePath, but can diverge in the future.
     */
    private void prepareGetValueTestData() {
        resourceAttributes = new HashMap<>();
        resourceAttributes.put("service.name", "prism-apm-parser");

        scopeMap = new HashMap<>();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("db.system", "mysql");

        spanMap = new HashMap<>();
        spanMap.put("attributes", attributes);
    }


    // --- Tests for getValue --- 

    /**
     * Verifies that getValue returns the value from the first valid path in the 'vals' list.
     */
    @Test
    public void getValue_shouldReturnValueFromFirstPathWhenItSucceeds() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule();
        outputRule.setVals(Arrays.asList("span-attributes__db.system", "resource-attributes__service.name"));

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(new HashMap<String, OtelOutputRule>() {{
            put("systemType", outputRule);
        }});

        String fieldName = "systemType";

        // Act
        String result = extractor.getValue(rule, fieldName, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("mysql", result);
    }

    /**
     * Verifies that getValue returns the value from the last valid path as a boundary condition.
     */
    @Test
    public void getValue_shouldReturnFromLastPathAsBoundary() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule();
        outputRule.setVals(Arrays.asList("span-attributes__invalid1", "span-attributes__invalid2", "span-attributes__db.system"));

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(Collections.singletonMap("systemType", outputRule));

        // Act
        String result = extractor.getValue(rule, "systemType", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("mysql", result);
    }

    /**
     * Verifies that getValue returns defaultVal if all paths in the 'vals' list fail
     */
    @Test
    public void getValue_shouldReturnNullWhenAllValsFail() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule();
        outputRule.setVals(Arrays.asList("span-attributes__invalid1", "span-attributes__invalid2"));
        outputRule.setDefaultVal("default"); // This should be returned

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(Collections.singletonMap("systemType", outputRule));

        // Act
        String result = extractor.getValue(rule, "systemType", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("default", result);
    }

    /**
     * Verifies that getValue returns the default value when the 'vals' list is missing.
     */
    @Test
    public void getValue_shouldReturnDefaultValWhenValsAreMissing() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule();
        outputRule.setDefaultVal("default-system");

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(Collections.singletonMap("systemType", outputRule));

        // Act
        String result = extractor.getValue(rule, "systemType", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("default-system", result);
    }

    /**
     * Verifies that getValue returns null when the rule has no 'vals' list and no default value.
     */
    @Test
    public void getValue_shouldReturnNullWhenNoValsOrDefaultVal() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule(); // No vals, no default

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(Collections.singletonMap("systemType", outputRule));

        // Act
        String result = extractor.getValue(rule, "systemType", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that getValue returns null when the requested field name does not exist in the rule.
     */
    @Test
    public void getValue_shouldReturnNullForNonExistentField() {
        // Arrange
        prepareGetValueTestData();

        OtelOutputRule outputRule = new OtelOutputRule();
        outputRule.setVals(Collections.singletonList("span-attributes__db.system"));

        OtelComponentRule rule = new OtelComponentRule();
        rule.setOutputs(Collections.singletonMap("systemType", outputRule));

        // Act
        String result = extractor.getValue(rule, "nonExistentField", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that getValue returns null when the provided rule is null.
     */
    @Test
    public void getValue_shouldReturnNullForNullRule() {
        // Arrange
        prepareGetValueTestData();

        // Act
        String result = extractor.getValue(null, "systemType", spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }


    // --- Tests for resolvePath --- 

    /**
     * Verifies that `resolvePath` can successfully extract a value from span attributes.
     */
    @Test
    public void resolvePath_shouldGetValueFromSpanAttributes() {
        // Arrange
        prepareResolvePathTestData();
        String path = "span-attributes__db.system";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("mysql", result);
    }

    /**
     * Verifies that `resolvePath` can extract a value from the top level of the span map.
     */
    @Test
    public void resolvePath_shouldGetValueFromSpanTopLevel() {
        // Arrange
        prepareResolvePathTestData();
        String path = "span__name";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("SELECT from users", result);
    }

    /**
     * Verifies that `resolvePath` can extract a value from resource attributes.
     */
    @Test
    public void resolvePath_shouldGetValueFromResourceAttributes() {
        // Arrange
        prepareResolvePathTestData();
        String path = "resource-attributes__service.name";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("prism-apm-parser", result);
    }

    /**
     * Verifies that `resolvePath` can extract a value from the scope map.
     */
    @Test
    public void resolvePath_shouldGetValueFromScope() {
        // Arrange
        prepareResolvePathTestData();
        String path = "spans-scope__name";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("io.opentelemetry.jdbc", result);
    }

    /**
     * Verifies that `resolvePath` correctly converts a numeric value to a String.
     */
    @Test
    public void resolvePath_shouldConvertNumberValueToString() {
        // Arrange
        prepareResolvePathTestData();
        String path = "span-attributes__http.response.status_code";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("200", result);
    }

    /**
     * Verifies that `resolvePath` returns null for a path with an invalid source prefix.
     */
    @Test
    public void resolvePath_shouldReturnNullForInvalidSourcePrefix() {
        // Arrange
        prepareResolvePathTestData();
        String path = "invalid-source__service.name";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that `resolvePath` returns null for a path that has fewer than two parts.
     */
    @Test
    public void resolvePath_shouldReturnNullForPathWithLessThanTwoParts() {
        // Arrange
        prepareResolvePathTestData();
        String path = "span";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that `resolvePath` returns null when the target source map (e.g., attributes) is null.
     */
    @Test
    public void resolvePath_shouldReturnNullWhenSourceMapIsNull() {
        // Arrange
        prepareResolvePathTestData();
        // Overwrite the specific condition for this test
        spanMap.put("attributes", null);
        String path = "span-attributes__db.system";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that `resolvePath` returns null when the final key is missing from the map.
     */
    @Test
    public void resolvePath_shouldReturnNullWhenFinalKeyIsMissing() {
        // Arrange
        prepareResolvePathTestData();
        String path = "span-attributes__db.user";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that `resolvePath` returns null when the path string itself is null.
     */
    @Test
    public void resolvePath_shouldReturnNullWhenPathIsNull() {
        // Arrange
        prepareResolvePathTestData();
        String path = null;

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }

    /**
     * Verifies that `resolvePath` returns null when the final resolved value is null.
     */
    @Test
    public void resolvePath_shouldReturnNullWhenFinalValueIsNull() {
        // Arrange
        prepareResolvePathTestData();
        // Overwrite the specific condition for this test
        ((Map<String, Object>) spanMap.get("attributes")).put("db.user", null);
        String path = "span-attributes__db.user";

        // Act
        String result = extractor.resolvePath(path, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result);
    }
}
