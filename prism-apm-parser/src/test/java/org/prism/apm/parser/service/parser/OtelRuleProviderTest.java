package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.OtelParserConfigLoader;
import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelParserConfig;
import org.prism.apm.parser.config.otel.OtelParserDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OtelRuleProviderTest {

    @Mock
    private OtelParserConfigLoader loader;

    @Mock
    private OtelValueExtractor valueExtractor;

    private OtelRuleProvider ruleProvider;

    // Test data can be empty as we rely on mocks
    private final Map<String, Object> spanMap = Collections.emptyMap();
    private final Map<String, Object> scopeMap = Collections.emptyMap();
    private final Map<String, Object> resourceAttributes = Collections.emptyMap();

    /**
     * Verifies that a rule with multiple conditions matches if only one of them is met (OR logic).
     */
    @Test
    public void findEffectiveRuleForSpan_shouldMatch_whenOnlyOneOfMultipleConditionsIsMet() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule multiConditionRule = new OtelComponentRule();
        multiConditionRule.setName("multi-condition-rule");
        Map<String, String> dbCondition = Collections.singletonMap("span-attributes__db.system", "mysql");
        Map<String, String> httpCondition = Collections.singletonMap("span-attributes__http.method", "GET");
        multiConditionRule.setMatch(Arrays.asList(dbCondition, httpCondition));

        OtelComponentRule defaultRule = new OtelComponentRule();
        defaultRule.setName("default");

        parserDef.setComponentDefinitions(Collections.singletonList(multiConditionRule));
        parserDef.setDefaults(defaultRule);

        when(loader.getOtelParserConfig()).thenReturn(config);
        when(valueExtractor.resolvePath(eq("span-attributes__db.system"), any(), any(), any()))
                .thenReturn(null); // FAIL
        when(valueExtractor.resolvePath(eq("span-attributes__http.method"), any(), any(), any()))
                .thenReturn("GET"); // PASS

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNotNull(effectiveRule);
        assertEquals("multi-condition-rule", effectiveRule.getName());
    }

    /**
     * Verifies that the first rule in the list wins if a span matches multiple rules.
     */
    @Test
    public void findEffectiveRuleForSpan_shouldReturnFirstRule_whenMultipleRulesMatch() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule dbRule = new OtelComponentRule();
        dbRule.setName("database");
        dbRule.setMatch(Collections.singletonList(Collections.singletonMap("span-attributes__db.system", ".*")));

        OtelComponentRule httpRule = new OtelComponentRule();
        httpRule.setName("http");
        httpRule.setMatch(Collections.singletonList(Collections.singletonMap("span-attributes__http.method", ".*")));

        parserDef.setComponentDefinitions(Arrays.asList(dbRule, httpRule)); // DB rule is first

        when(loader.getOtelParserConfig()).thenReturn(config);
        // This span satisfies both rules
        when(valueExtractor.resolvePath(eq("span-attributes__db.system"), any(), any(), any())).thenReturn("mysql");

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("database", effectiveRule.getName(), "The first matching rule (database) should have been returned");
    }

    /**
     * Verifies that the defaults rule is returned if no component rules match.
     */
    @Test
    public void findEffectiveRuleForSpan_shouldReturnDefaultRule_whenNoRulesMatch() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule dbRule = new OtelComponentRule();
        dbRule.setName("database");
        dbRule.setMatch(Collections.singletonList(Collections.singletonMap("span-attributes__db.system", "mysql")));
        parserDef.setComponentDefinitions(Collections.singletonList(dbRule));

        OtelComponentRule defaultRule = new OtelComponentRule();
        defaultRule.setName("default");
        parserDef.setDefaults(defaultRule);

        when(loader.getOtelParserConfig()).thenReturn(config);
        // This value does not match the rule
        when(valueExtractor.resolvePath(any(), any(), any(), any())).thenReturn("redis");

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("default", effectiveRule.getName(), "The default rule should be returned when no component rules match");
    }

    /**
     * Verifies that a regex with specific values (e.g., "POST|PUT") is evaluated correctly.
     */
    @Test
    public void findEffectiveRuleForSpan_shouldMatch_withSpecificRegex() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule httpWriteRule = new OtelComponentRule();
        httpWriteRule.setName("http-write");
        httpWriteRule.setMatch(Collections.singletonList(Collections.singletonMap("span-attributes__http.method", "POST|PUT")));
        parserDef.setComponentDefinitions(Collections.singletonList(httpWriteRule));

        when(loader.getOtelParserConfig()).thenReturn(config);
        when(valueExtractor.resolvePath(eq("span-attributes__http.method"), any(), any(), any())).thenReturn("PUT");

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("http-write", effectiveRule.getName(), "Rule with regex 'POST|PUT' should match value 'PUT'");
    }

    /**
     * Verifies that the wildcard regex ".*" matches any non-null value.
     */
    @Test
    public void findEffectiveRuleForSpan_shouldMatch_withWildcardRegex() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule dbRule = new OtelComponentRule();
        dbRule.setName("database");
        dbRule.setMatch(Collections.singletonList(Collections.singletonMap("span-attributes__db.system", ".*")));
        parserDef.setComponentDefinitions(Collections.singletonList(dbRule));

        when(loader.getOtelParserConfig()).thenReturn(config);
        when(valueExtractor.resolvePath(eq("span-attributes__db.system"), any(), any(), any())).thenReturn("any_db_system");

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("database", effectiveRule.getName(), "Rule with wildcard regex should match any value");
    }

    /**
     * Verifies that the default rule is returned if the componentDefinitions list is null.
     */
    @Test
    public void findEffectiveRuleForSpan_shouldReturnDefaultRule_whenComponentDefinitionsIsNull() {
        // Arrange
        OtelParserConfig config = new OtelParserConfig();
        OtelParserDefinition parserDef = new OtelParserDefinition();
        config.setParser(parserDef);

        OtelComponentRule defaultRule = new OtelComponentRule();
        defaultRule.setName("default");
        parserDef.setDefaults(defaultRule);
        parserDef.setComponentDefinitions(null); // Explicitly set to null

        when(loader.getOtelParserConfig()).thenReturn(config);

        ruleProvider = new OtelRuleProvider(loader, valueExtractor);

        // Act
        OtelComponentRule effectiveRule = ruleProvider.findEffectiveRuleForSpan(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("default", effectiveRule.getName(), "Default rule should be returned if component definitions are null");
    }
}
