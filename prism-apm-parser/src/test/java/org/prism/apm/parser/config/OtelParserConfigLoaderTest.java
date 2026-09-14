package org.prism.apm.parser.config;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelOutputRule;
import org.prism.apm.parser.config.otel.OtelParserConfig;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

public class OtelParserConfigLoaderTest {

    // --- Tests for basic loading and validation ---

    @Test
    public void testSuccessfulLoadAndParse() throws FileNotFoundException {
        OtelParserConfigLoader loader = new OtelParserConfigLoader("config-test-cases/test-parser-component-config.yml");
        OtelParserConfig config = loader.getOtelParserConfig();

        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getParser(), "Parser config should not be null");
        assertEquals("1.0", config.getVersion());
        assertNotNull(config.getParser().getComponentDefinitions(), "Component definitions should not be null");
        assertEquals(4, config.getParser().getComponentDefinitions().size(), "There should be 4 component definitions");
        assertNotNull(config.getParser().getDefaults(), "Defaults should not be null");
        assertNotNull(config.getParser().getDefaults().getOutputs(), "Default outputs should not be null");
    }

    @Test
    public void testLoadConfig_shouldThrowException_whenVersionIsMissing() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-invalid-version.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldThrowException_whenParserIsMissing() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-missing-parser.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldThrowException_whenParserIsEmpty() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-empty-parser.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldThrowException_whenMatchIsMissing() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-invalid-match.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldThrowException_forDuplicateComponentNames() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-duplicate-component-names.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldThrowException_whenOutputFieldIsInvalid() {
        assertThrows(IllegalStateException.class, () -> {
            new OtelParserConfigLoader("config-test-cases/test-invalid-output-field.yml");
        });
    }

    @Test
    public void testLoadConfig_shouldSucceed_whenAllAllowedFieldsAreUsed() throws FileNotFoundException {
        OtelParserConfigLoader loader = new OtelParserConfigLoader("config-test-cases/test-all-output-fields.yml");
        OtelParserConfig config = loader.getOtelParserConfig();
        OtelComponentRule rule = config.getParser().getComponentDefinitions().get(0);
        assertEquals(8, rule.getOutputs().size(), "The outputs map should contain all 8 allowed fields");
    }

    // --- Tests for merge logic (Deep Merge) ---

    private OtelParserConfig loadCombinationsTestConfig() throws java.io.FileNotFoundException {
        return new OtelParserConfigLoader("config-test-cases/test-merge-combinations.yml").getOtelParserConfig();
    }

    private OtelComponentRule findRule(OtelParserConfig config, String ruleName) {
        return config.getParser().getComponentDefinitions().stream()
                .filter(rule -> ruleName.equals(rule.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Rule " + ruleName + " not found"));
    }

    /**
     * Tests that if a component rule defines `vals` but not `defaultVal`,
     * the `defaultVal` from the corresponding default rule will be merged in.
     */
    @Test
    public void testMerge_shouldAddDefaultVal_whenCompHasVals() throws Exception {
        OtelParserConfig config = loadCombinationsTestConfig();
        OtelComponentRule rule = findRule(config, "test-component");
        OtelOutputRule output = rule.getOutputs().get("actionName");

        assertNotNull(output);
        assertEquals("component-action", output.getVals().get(0), "Component's own vals should be preserved");
        assertNotNull(output.getDefaultVal(), "Default's defaultVal should be merged");
        assertEquals("default-action-should-be-ignored", output.getDefaultVal());
    }

    /**
     * Tests that if a component rule defines `defaultVal` but not `vals`,
     * the `vals` from the corresponding default rule will be merged in.
     */
    @Test
    public void testMerge_shouldAddVals_whenCompHasDefaultVal() throws Exception {
        OtelParserConfig config = loadCombinationsTestConfig();
        OtelComponentRule rule = findRule(config, "test-component");
        OtelOutputRule output = rule.getOutputs().get("targetName");

        assertNotNull(output);
        assertEquals("component-target", output.getDefaultVal(), "Component's own defaultVal should be preserved");
        assertNotNull(output.getVals(), "Default's vals should be merged");
        assertEquals(1, output.getVals().size());
        assertEquals("default-target-should-be-ignored", output.getVals().get(0));
    }

    /**
     * Tests that if a component rule defines both `vals` and `defaultVal`,
     * it remains completely unchanged, ignoring the default rule.
     */
    @Test
    public void testMerge_shouldNotChange_whenCompHasBoth() throws Exception {
        OtelParserConfig config = loadCombinationsTestConfig();
        OtelComponentRule rule = findRule(config, "test-component");
        OtelOutputRule output = rule.getOutputs().get("component");

        assertNotNull(output);
        assertEquals("component-component-val", output.getVals().get(0));
        assertEquals("component-component-default", output.getDefaultVal());
    }

    /**
     * Tests that if a component rule for a field is completely missing,
     * the rule from `defaults` is copied over wholesale. This test checks three sub-cases:
     *  - Default has `vals` only (`systemType`)
     *  - Default has `defaultVal` only (`spanKind`)
     *  - Default has both (`error`)
     */
    @Test
    public void testMerge_shouldCopyFullRule_whenCompHasNone() throws Exception {
        OtelParserConfig config = loadCombinationsTestConfig();
        OtelComponentRule rule = findRule(config, "test-component");
        
        // Test for systemType (default has vals only)
        OtelOutputRule systemTypeOutput = rule.getOutputs().get("systemType");
        assertNotNull(systemTypeOutput);
        assertEquals("default-system", systemTypeOutput.getVals().get(0));
        assertNull(systemTypeOutput.getDefaultVal());

        // Test for spanKind (default has defaultVal only)
        OtelOutputRule spanKindOutput = rule.getOutputs().get("spanKind");
        assertNotNull(spanKindOutput);
        assertNull(spanKindOutput.getVals());
        assertEquals("default-kind", spanKindOutput.getDefaultVal());

        // Test for error (default has both)
        OtelOutputRule errorOutput = rule.getOutputs().get("error");
        assertNotNull(errorOutput);
        assertEquals("1", errorOutput.getVals().get(0));
        assertEquals("0", errorOutput.getDefaultVal());
    }

    @Test
    public void testAllValues() throws FileNotFoundException {
        OtelParserConfigLoader loader = new OtelParserConfigLoader("config-test-cases/test-all-values.yml");
        OtelParserConfig config = loader.getOtelParserConfig();
        OtelComponentRule rule = config.getParser().getComponentDefinitions().get(0);

        // Check the match field
        assertNotNull(rule.getMatch());
        assertEquals(1, rule.getMatch().size());
        assertTrue(rule.getMatch().get(0).containsKey("empty_key"));
        assertEquals("", rule.getMatch().get(0).get("empty_key"));

        // Case 1: vals is null
        OtelOutputRule serviceNameRule = rule.getOutputs().get("serviceName");
        assertNotNull(serviceNameRule);
        assertNull(serviceNameRule.getVals());
        assertNull(serviceNameRule.getDefaultVal());

        // Case 2: vals list contains an empty string
        OtelOutputRule spanKindRule = rule.getOutputs().get("spanKind");
        assertNotNull(spanKindRule);
        assertEquals(1, spanKindRule.getVals().size());
        assertEquals("", spanKindRule.getVals().get(0));
        assertNull(spanKindRule.getDefaultVal());

        // Case 3: defaultVal is an empty string
        OtelOutputRule actionNameRule = rule.getOutputs().get("actionName");
        assertNotNull(actionNameRule);
        assertEquals("", actionNameRule.getDefaultVal());
        assertNull(actionNameRule.getVals());

        // Case 4: Value contains special characters
        OtelOutputRule targetNameRule = rule.getOutputs().get("targetName");
        assertNotNull(targetNameRule);
        assertEquals("specialchars", targetNameRule.getVals().get(0));
        assertNull(targetNameRule.getDefaultVal());

        // Case 5: vals list contains multiple values
        OtelOutputRule componentRule = rule.getOutputs().get("component");
        assertNotNull(componentRule);
        assertEquals(3, componentRule.getVals().size());
        assertEquals("value1", componentRule.getVals().get(0));
        assertEquals("value2", componentRule.getVals().get(1));
        assertEquals("value3", componentRule.getVals().get(2));
        assertNull(componentRule.getDefaultVal());

        // Case 6: Both vals and defaultVal are present
        OtelOutputRule errorRule = rule.getOutputs().get("error");
        assertNotNull(errorRule);
        assertEquals("error-val", errorRule.getVals().get(0));
        assertEquals("error-default", errorRule.getDefaultVal());
    }
}
