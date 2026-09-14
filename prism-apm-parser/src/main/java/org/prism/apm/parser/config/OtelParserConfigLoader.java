package org.prism.apm.parser.config;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.config.otel.OtelOutputRule;
import org.prism.apm.parser.config.otel.OtelParserConfig;
import org.prism.apm.parser.constants.SpanInfoFields;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads, validates, and pre-processes the OpenTelemetry parser configuration from a YAML file.
 */
@Component
@Slf4j
@Getter
public class OtelParserConfigLoader {

    private final OtelParserConfig otelParserConfig;

    public OtelParserConfigLoader() throws FileNotFoundException {
        this("parser-component-config.yml");
    }

    /**
     * Constructor that loads a specific configuration file.
     * This is used for loading the main config in production and test-specific configs during unit tests.
     * The process involves loading the YAML, validating its structure and content, and then merging default values.
     * @param configFileName The name of the YAML configuration file to load from the classpath.
     * @throws FileNotFoundException if the specified config file is not found.
     * @throws IllegalStateException if the configuration fails validation.
     */
    public OtelParserConfigLoader(String configFileName) throws FileNotFoundException {
        log.info("Loading {}...", configFileName);

        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(configFileName);

        if (inputStream == null) {
            log.error("FATAL: {} not found in classpath. Parser will not work.", configFileName);
            throw new FileNotFoundException(configFileName + " not found.");
        }

        this.otelParserConfig = yaml.loadAs(inputStream, OtelParserConfig.class);

        // Validate the loaded configuration
        validateConfig(this.otelParserConfig);

        // Post-process the configuration to merge defaults
        mergeDefaultsIntoRules(this.otelParserConfig);
        log.info("Successfully loaded and processed {}.", configFileName);
    }

    /**
     * Validates the structural integrity and content of the loaded parser configuration.
     * @param config The {@link OtelParserConfig} object to validate.
     * @throws IllegalStateException if any validation rule is violated.
     */
    private void validateConfig(OtelParserConfig config) {
        if (config.getVersion() == null || config.getVersion().isEmpty()) {
            throw new IllegalStateException("Config validation failed: 'version' cannot be null or empty.");
        }
        if (config.getParser() == null) {
            throw new IllegalStateException("Config validation failed: 'parser' section cannot be null.");
        }
        if ((config.getParser().getComponentDefinitions() == null || config.getParser().getComponentDefinitions().isEmpty()) &&
            config.getParser().getDefaults() == null) {
            throw new IllegalStateException("Config validation failed: 'parser' must have at least 'componentDefinitions' or 'defaults'.");
        }

        // Validate component rules
        if (config.getParser().getComponentDefinitions() != null) {
            java.util.Set<String> seenNames = new java.util.HashSet<>();
            for (OtelComponentRule rule : config.getParser().getComponentDefinitions()) {

                // check if name is empty
                if (rule.getName() == null || rule.getName().isEmpty()) {
                    throw new IllegalStateException("Config validation failed: component rule name cannot be null or empty.");
                }

                // check duplicate name
                if (!seenNames.add(rule.getName())) {
                    throw new IllegalStateException(
                        "Config validation failed: Duplicate component definition name found: '" + rule.getName() + "'."
                    );
                }

                // check if match is empty
                if (rule.getMatch() == null || rule.getMatch().isEmpty()) {
                    throw new IllegalStateException(
                        "Config validation failed: 'match' cannot be null or empty for component rule '" + rule.getName() + "'."
                    );
                }
                validateOutputFields(rule.getOutputs(), "component rule '" + rule.getName() + "'");
            }
        }

        // Validate defaults rule
        if (config.getParser().getDefaults() != null) {
            validateOutputFields(config.getParser().getDefaults().getOutputs(), "'defaults' section");
        }
    }

    /**
     * Helper method to validate that all field names used in an 'outputs' map are allowed.
     * @param outputs The map of field names to output rules.
     * @param source A string indicating the source of the outputs map (e.g., a component name or 'defaults') for clear error messages.
     * @throws IllegalStateException if an invalid field name is found.
     */
    private void validateOutputFields(Map<String, OtelOutputRule> outputs, String source) {
        if (outputs == null) {
            return;
        }
        for (String fieldName : outputs.keySet()) {
            if (!SpanInfoFields.ALLOWED_FIELDS.contains(fieldName)) {
                throw new IllegalStateException(
                    "Config validation failed: Invalid output field '" + fieldName + "' in " + source + ". " +
                    "Allowed fields are: " + SpanInfoFields.ALLOWED_FIELDS
                );
            }
        }
    }

    /**
     * Merges rules from the 'defaults' section into each component definition.
     * This is a "deep" or "property-level" merge.
     * <p>
     * The logic is as follows:
     * <ul>
     *   <li>- If a component rule for a field is missing entirely, the default rule is copied wholesale.</li>
     *   <li>- If a component rule exists, it will be supplemented with `vals` or `defaultVal` from the default rule, but only if the component rule is missing that specific property.</li>
     *   <li>- Existing properties on a component rule are never overwritten.</li>
     * </ul>
     * @param config The configuration object containing the rules to be merged.
     */
    private void mergeDefaultsIntoRules(OtelParserConfig config) {
        if (config.getParser() == null || config.getParser().getDefaults() == null) {
            log.warn("Defaults section is null. Skipping default merge.");
            return;
        }
        if (config.getParser().getComponentDefinitions() == null) {
            return; // Nothing to merge into
        }

        OtelComponentRule defaultRule = config.getParser().getDefaults();
        if (defaultRule.getOutputs() == null || defaultRule.getOutputs().isEmpty()) {
            return; // Nothing to merge
        }

        log.info("Merging default rules into component rules...");

        // For each rule defined in defaults...
        for (Map.Entry<String, OtelOutputRule> defaultEntry : defaultRule.getOutputs().entrySet()) {
            String fieldName = defaultEntry.getKey();
            OtelOutputRule defaultOutputRule = defaultEntry.getValue();

            // ...apply it to every component definition.
            for (OtelComponentRule componentRule : config.getParser().getComponentDefinitions()) {
                if (componentRule.getOutputs() == null) {
                    componentRule.setOutputs(new java.util.HashMap<>());
                }

                OtelOutputRule componentOutputRule = componentRule.getOutputs().get(fieldName);

                if (componentOutputRule == null) {
                    // Case 1: Component does not have the rule. Copy the default rule wholesale.
                    componentRule.getOutputs().put(fieldName, defaultOutputRule);
                } else {
                    // Case 2: Component has a rule. Perform property-level merge.
                    if (componentOutputRule.getVals() == null && defaultOutputRule.getVals() != null) {
                        componentOutputRule.setVals(defaultOutputRule.getVals());
                    }
                    if (componentOutputRule.getDefaultVal() == null && defaultOutputRule.getDefaultVal() != null) {
                        componentOutputRule.setDefaultVal(defaultOutputRule.getDefaultVal());
                    }
                }
            }
        }
        log.info("Default rules merged successfully.");
    }
}