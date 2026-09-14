package org.prism.apm.parser.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.prism.apm.parser.model.otel.OtelResource;
import org.prism.apm.parser.model.otel.OtelResourceSpan;
import org.prism.apm.parser.model.otel.OtelScopeSpan;
import org.prism.apm.parser.model.otel.OtelTraceData;
import org.prism.apm.parser.util.LoadTraceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenTelemetryTraceDeserializerTest {

    private OpenTelemetryTraceDeserializer deserializer;

    private static final String VALID_TRACE_JSON = LoadTraceUtil.loadTraceFromResource("sample-traces/simplified-error-trace");

    @BeforeEach
    void setUp() {
        deserializer = new OpenTelemetryTraceDeserializer();
    }

    /**
     * Test Case 1: Valid JSON should be deserialized successfully and completely
     */
    @Test
    void deserialize_testDeserializeSuccessDeepValidation() throws JsonProcessingException {
        OtelTraceData result = deserializer.deserialize(VALID_TRACE_JSON);

        // Top-level: OtelTraceData
        assertNotNull(result, "Top-level OtelTraceData object should not be null");
        List<OtelResourceSpan> resourceSpans = result.getResourceSpans();
        assertNotNull(resourceSpans, "ResourceSpans list should not be null");
        assertEquals(1, resourceSpans.size(), "ResourceSpans list size should be 1");

        // Level 1: OtelResourceSpan
        OtelResourceSpan resourceSpan = resourceSpans.get(0);
        assertNotNull(resourceSpan, "The first OtelResourceSpan should not be null");
        assertEquals("https://opentelemetry.io/schemas/1.24.0", resourceSpan.getSchemaUrl(), "SchemaUrl should match");

        // Level 2: OtelResource
        OtelResource resource = resourceSpan.getResource();
        assertNotNull(resource, "Resource should not be null");
        Map<String, Object> resourceAttributes = resource.getAttributes();
        assertNotNull(resourceAttributes, "Resource attributes should not be null");
        assertEquals(2, resourceAttributes.size(), "Resource attributes size should be 2");
        assertEquals("783414b39580d6484d869cadf16fa9e3504b78faaa4a8cfa595b859624fd1bf3", resourceAttributes.get("container.id"));
        assertEquals(
                Lists.newArrayList("/usr/local/openjdk-17/bin/java", "-jar", "/app/sample-provider-service-v2-1.0.0-SNAPSHOT.jar", "--spring.profiles.active=docker"),
                resourceAttributes.get("process.command_args")
        );

        // Level 2: ScopeSpans list
        List<OtelScopeSpan> scopeSpans = resourceSpan.getScopeSpans();
        assertNotNull(scopeSpans, "ScopeSpans list should not be null");
        assertEquals(2, scopeSpans.size(), "ScopeSpans list size should be 2");

        // --- First ScopeSpan (JDBC) ---
        OtelScopeSpan jdbcScopeSpan = scopeSpans.get(0);
        assertEquals("io.opentelemetry.jdbc", jdbcScopeSpan.getScope().get("name"));
        assertEquals("2.18.1-alpha", jdbcScopeSpan.getScope().get("version"));
        assertEquals(1, jdbcScopeSpan.getSpans().size(), "JDBC Scope should have 1 Span");

        Map<String, Object> jdbcSpan = jdbcScopeSpan.getSpans().get(0);
        assertEquals(11, jdbcSpan.size(), "JDBC Span should have 11 top-level fields");
        assertEquals("cda5dd2887c0bdca97047dfe4aeaafe6", jdbcSpan.get("traceId"));
        assertEquals("2452cc21d05537fe", jdbcSpan.get("spanId"));
        assertEquals("d88d77a0b2056d64", jdbcSpan.get("parentSpanId"));
        assertEquals(257, jdbcSpan.get("flags"));
        assertEquals("INSERT mydatabase.user22222", jdbcSpan.get("name"));
        assertEquals(3, jdbcSpan.get("kind"));
        assertEquals("1754291710722881527", jdbcSpan.get("startTimeUnixNano"));
        assertEquals("1754291710735923937", jdbcSpan.get("endTimeUnixNano"));
        assertNotNull(jdbcSpan.get("attributes"));
        assertNotNull(jdbcSpan.get("events"));
        assertNotNull(jdbcSpan.get("status"));

        Map<String, Object> jdbcSpanAttributes = (Map<String, Object>) jdbcSpan.get("attributes");
        assertNotNull(jdbcSpanAttributes, "JDBC Span attributes should not be null");
        assertEquals(1, jdbcSpanAttributes.size());
        assertEquals("span-mysql", jdbcSpanAttributes.get("server.address"));

        List<Map<String, Object>> jdbcEvents = (List<Map<String, Object>>) jdbcSpan.get("events");
        assertNotNull(jdbcEvents, "JDBC Span events list should not be null");
        assertEquals(1, jdbcEvents.size());
        Map<String, Object> jdbcEvent = jdbcEvents.get(0);
        assertEquals("exception", jdbcEvent.get("name"));
        List<Map<String, Object>> jdbcEventAttributes = (List<Map<String, Object>>) jdbcEvent.get("attributes");
        assertEquals("exception.message", jdbcEventAttributes.get(0).get("key"));
        assertEquals("Table 'mydatabase.user22222' doesn't exist", ((Map<String, Object>)jdbcEventAttributes.get(0).get("value")).get("stringValue"));
        assertEquals("exception.type", jdbcEventAttributes.get(1).get("key"));
        assertEquals("java.sql.SQLSyntaxErrorException", ((Map<String, Object>)jdbcEventAttributes.get(1).get("value")).get("stringValue"));
        assertEquals("exception.stacktrace", jdbcEventAttributes.get(2).get("key"));
        assertEquals("java.sql.SQLSyntaxErrorException: Table 'mydatabase.user22222' doesn't exist", ((Map<String, Object>)jdbcEventAttributes.get(2).get("value")).get("stringValue"));

        assertEquals(2, ((Map<String, Object>)jdbcSpan.get("status")).get("code"));

        // --- Second ScopeSpan (Tomcat) ---
        OtelScopeSpan tomcatScopeSpan = scopeSpans.get(1);
        assertEquals("io.opentelemetry.tomcat-10.0", tomcatScopeSpan.getScope().get("name"));
        assertEquals("2.18.1-alpha", tomcatScopeSpan.getScope().get("version"));
        assertEquals(1, tomcatScopeSpan.getSpans().size(), "Tomcat Scope should have 1 Span");

        Map<String, Object> tomcatSpan = tomcatScopeSpan.getSpans().get(0);
        assertEquals(11, tomcatSpan.size(), "Tomcat Span should have 11 top-level fields");
        assertEquals("cda5dd2887c0bdca97047dfe4aeaafe6", tomcatSpan.get("traceId"));
        assertEquals("d88d77a0b2056d64", tomcatSpan.get("spanId"));
        assertEquals("c31b2c9885296ab4", tomcatSpan.get("parentSpanId"));
        assertEquals(769, tomcatSpan.get("flags"));
        assertEquals("GET /middleware/exception", tomcatSpan.get("name"));
        assertEquals(2, tomcatSpan.get("kind"));
        assertEquals("1754291710677385361", tomcatSpan.get("startTimeUnixNano"));
        assertEquals("1754291710850743627", tomcatSpan.get("endTimeUnixNano"));
        assertNotNull(tomcatSpan.get("attributes"));
        assertNotNull(tomcatSpan.get("events"));
        assertNotNull(tomcatSpan.get("status"));

        Map<String, Object> tomcatSpanAttributes = (Map<String, Object>) tomcatSpan.get("attributes");
        assertNotNull(tomcatSpanAttributes, "Tomcat Span attributes should not be null");
        assertEquals(1, tomcatSpanAttributes.size());
        assertEquals("GET", tomcatSpanAttributes.get("http.request.method"));

    }

    /**
     * Test Case 2: Malformed JSON (e.g. trailing comma) should throw an exception.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"key\": \"value\",}",  // Malformed JSON (trailing comma)
            "",                      // Empty string
            "  ",                    // Blank string
            "just text",             // Not JSON at all
            "["                      // Incomplete JSON
    })
    void deserialize_testInvalidJsonInputThrowsException(String invalidInput) {
        assertThrows(JsonProcessingException.class, () -> {
            deserializer.deserialize(invalidInput);
        }, "Deserializing invalid JSON input '" + invalidInput + "' should throw JsonProcessingException");
    }

    /**
     * Test Case 3: Null input should return null
     */
    @Test
    void deserialize_testDeserializeNullInputReturnsNull() throws JsonProcessingException {
        OtelTraceData result = deserializer.deserialize(null);
        assertNull(result, "Result should be null for null input");
    }


    /**
     * Test Case 4: JSON that is structurally valid but missing required fields should throw an exception.
     */
    @Test
    void deserialize_testMissingRequiredFieldThrowsException() {
        String jsonWithoutRequiredField = "{\"someOtherField\": \"someValue\"}";
        assertThrows(JsonProcessingException.class, () -> {
            deserializer.deserialize(jsonWithoutRequiredField);
        }, "Deserializing JSON without the required 'resourceSpans' field should throw an exception.");
    }

    /**
     * Test Case 5: JSON with unknown properties should still succeed
     */
    @Test
    void deserialize_testDeserializeWithUnknownPropertySuccess() throws JsonProcessingException {
        String jsonWithUnknown = "{\"unknownField\":\"someValue\", \"resourceSpans\":[]}";

        OtelTraceData result = deserializer.deserialize(jsonWithUnknown);

        assertNotNull(result, "Result should not be null for JSON with unknown properties");
        assertNotNull(result.getResourceSpans(), "resourceSpans should be an empty list, not null");
        assertEquals(0, result.getResourceSpans().size());
    }
}