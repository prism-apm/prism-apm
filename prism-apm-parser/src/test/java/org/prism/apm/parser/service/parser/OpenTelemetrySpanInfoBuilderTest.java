package org.prism.apm.parser.service.parser;

import org.prism.apm.parser.config.otel.OtelComponentRule;
import org.prism.apm.parser.constants.SpanInfoFields;
import org.prism.apm.parser.enums.SpanKindEnum;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.init.bo.SpanFieldReplacer;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenTelemetrySpanInfoBuilderTest {

    @Mock
    private OtelRuleProvider ruleProvider;

    @Mock
    private OtelValueExtractor valueExtractor;

    @Mock
    private SpanFieldReplacer replacer;

    private OpenTelemetrySpanInfoBuilder spanInfoBuilder;

    private OtelComponentRule mockRule;
    private Map<String, Object> spanMap;
    private Map<String, Object> scopeMap;
    private Map<String, Object> resourceAttributes;

    @BeforeEach
    public void setUp() {
        // Arrange: Common setup for all tests
        spanInfoBuilder = new OpenTelemetrySpanInfoBuilder(ruleProvider, valueExtractor, replacer);

        mockRule = new OtelComponentRule();
        mockRule.setName("test-rule");

        spanMap = new HashMap<>();
        spanMap.put("traceId", "test-trace-id");
        spanMap.put("spanId", "test-span-id");
        spanMap.put("parentSpanId", "test-parent-id");
        spanMap.put("startTimeUnixNano", "1700000000000001000");
        spanMap.put("endTimeUnixNano", "1700000000101001000"); // 100ms duration

        scopeMap = Collections.singletonMap("scopeKey", "scopeValue");
        resourceAttributes = Collections.singletonMap("resourceKey", "resourceValue");

        // Mock the rule provider to always return our test rule
//        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
    }

    /**
     * return StrHappy path test to ensure a SpanInfo object is fully built when all dependencies return values.
     */
    @Test
    public void build_testBuildHappyPath() {
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);

        // Arrange
        when(valueExtractor.getValue(eq(mockRule), eq("serviceName"), any(), any(), any())).thenReturn("test-service");
        when(valueExtractor.getValue(eq(mockRule), eq("hostName"), any(), any(), any())).thenReturn("test-host");
        when(valueExtractor.getValue(eq(mockRule), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.INTERNAL.getValue()));
        when(valueExtractor.getValue(eq(mockRule), eq("actionName"), any(), any(), any())).thenReturn("test-action");
        when(valueExtractor.getValue(eq(mockRule), eq("targetName"), any(), any(), any())).thenReturn("test-target");
        when(valueExtractor.getValue(eq(mockRule), eq("component"), any(), any(), any())).thenReturn("test-component");
        when(valueExtractor.getValue(eq(mockRule), eq("systemType"), any(), any(), any())).thenReturn("test-system");
        when(valueExtractor.getValue(eq(mockRule), eq("error"), any(), any(), any())).thenReturn("0");

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("test-service", spanInfo.getServiceName(), "Service name should be correctly set");

        assertEquals("test-host", spanInfo.getHostName(), "Host name should be correctly set");

        assertEquals(SpanKindEnum.INTERNAL.getValue(), spanInfo.getSpanKind().shortValue(), "Span kind should be correctly set");

        assertEquals("test-action", spanInfo.getActionName(), "Action name should be correctly set");

        assertEquals("test-target", spanInfo.getTargetName(), "Target name should be correctly set");

        assertEquals("test-component", spanInfo.getComponent(), "Component should be correctly set");

        assertEquals("test-system", spanInfo.getSystemType(), "System type should be correctly set");

        assertEquals(Long.valueOf(0), spanInfo.getError(), "Error status should be correctly set");



        assertEquals("test-trace-id", spanInfo.getTraceId(), "Trace ID should be set directly from span data");

        assertEquals("test-span-id", spanInfo.getSpanId(), "Span ID should be set directly from span data");

        assertEquals("test-parent-id", spanInfo.getParentSpanId(), "Parent Span ID should be set directly from span data");

        assertEquals(Long.valueOf(101000), spanInfo.getDuration(), "Duration should be calculated correctly in milliseconds");

        assertEquals(Long.valueOf(TimeUnit.NANOSECONDS.toMicros(1700000000000001000L)), spanInfo.getTime(), "Time should be calculated correctly in milliseconds");
    }

    /**
     * return StrTest the getError method to verify it correctly parses a non-null string value ("1") into a Long.
     */
    @Test
    public void build_testGetErrorWithValue() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.UNKNOWN.getValue()));
        when(valueExtractor.getValue(eq(mockRule), eq("error"), any(), any(), any())).thenReturn("1");

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals(Long.valueOf(1), spanInfo.getError(), "Error code should be 1");
    }

    /**
     * return StrTest the getError method to verify it correctly returns the default value 0L when the value is null.
     */
    @Test
    public void build_testGetErrorWithNullValue() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.UNKNOWN.getValue()));
        when(valueExtractor.getValue(eq(mockRule), eq("error"), any(), any(), any())).thenReturn(null);

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals(Long.valueOf(0), spanInfo.getError(), "Error code should be the default value of 0");
    }

    /**
     * return StrTest the getRemoteAddr method for a SERVER span kind, successfully finding client.address.
     */
    @Test
    public void build_testGetRemoteAddrAsServer() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.SERVER.getValue()));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("client.address", "192.168.1.100");
        attributes.put("server.address", "10.0.0.1"); // This should be ignored
        spanMap.put("attributes", attributes);

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("192.168.1.100", spanInfo.getRemoteAddr(), "Remote address should be the value of client.address for a SERVER span");
    }

    /**
     * return StrTest the getRemoteAddr method for a CLIENT span kind, successfully finding server.address.
     */
    @Test
    public void build_testGetRemoteAddrAsClient() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.CLIENT.getValue()));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("client.address", "192.168.1.100"); // This should be ignored
        attributes.put("server.address", "10.0.0.1");
        spanMap.put("attributes", attributes);

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("10.0.0.1", spanInfo.getRemoteAddr(), "Remote address should be the value of server.address for a CLIENT span");
    }

    /**
     * return StrTest the getRemoteAddr fallback logic to network.peer.address when client.address is missing for a SERVER span.
     */
    @Test
    public void build_testGetRemoteAddrFallbackToPeerAddressForServer() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.SERVER.getValue()));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("network.peer.address", "127.0.0.1");
        spanMap.put("attributes", attributes);

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("127.0.0.1", spanInfo.getRemoteAddr(), "Remote address should fall back to network.peer.address");
    }

    /**
     * return StrTest the getRemoteAddr fallback logic to network.peer.address when server.address is missing for a CLIENT span.
     */
    @Test
    public void build_testGetRemoteAddrFallbackToPeerAddressForClient() {
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub

        // Arrange
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.CLIENT.getValue()));
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("network.peer.address", "127.0.0.1");
        spanMap.put("attributes", attributes);

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals("127.0.0.1", spanInfo.getRemoteAddr(), "Remote address should fall back to network.peer.address");
    }

    /**
     * return StrTest the getRemoteAddr method to ensure it returns null when no relevant address attributes are found.
     */
    @Test
    public void build_testGetRemoteAddrNoAddressFound() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(mockRule);
        when(valueExtractor.getValue(any(), any(), any(), any(), any())).thenReturn(null); // Default stub
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.SERVER.getValue()));
        spanMap.put("attributes", Collections.singletonMap("unrelated.attribute", "some-value"));

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals(Strings.EMPTY, spanInfo.getRemoteAddr(), "Remote address should be empty string when no relevant attributes are found");
    }

    public void build_testGetRemoteAddrNoAttributesMap() {
        // Arrange
        when(valueExtractor.getValue(any(), eq("spanKind"), any(), any(), any())).thenReturn(String.valueOf(SpanKindEnum.SERVER.getValue()));
        spanMap.put("attributes", null); // No attributes map

        // Act
        SpanInfo spanInfo = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals(Strings.EMPTY, spanInfo.getRemoteAddr(), "Remote address should be empty string when the attributes map is null");
    }

    /**
     * Test that the build method returns null when no matching rule is found.
     */
    @Test
    public void build_shouldReturnNullWhenNoRuleFound() {
        // Arrange
        when(ruleProvider.findEffectiveRuleForSpan(any(), any(), any())).thenReturn(null);

        // Act
        SpanInfo result = spanInfoBuilder.build(spanMap, scopeMap, resourceAttributes);

        // Assert
        assertNull(result, "Should return null when no effective rule is found");
    }

    @Test
    public void getActionName_whenReplacerReturnsValue_returnsReplacedValue() {
        String originalAction = "originalAction";
        String replacedAction = "replacedAction";
        when(valueExtractor.getValue(any(), eq(SpanInfoFields.ACTION_NAME), any(), any(), any())).thenReturn(originalAction);
        when(replacer.match(any(), any(), eq(SpanInfoFields.ACTION_NAME), eq(originalAction))).thenReturn(replacedAction);

        String actionName = spanInfoBuilder.getActionName(mockRule, spanMap, scopeMap, resourceAttributes);

        assertEquals(replacedAction, actionName);
    }

    @Test
    public void getActionName_whenReplacerReturnsNull_returnsOriginalValue() {
        String originalAction = "originalAction";
        when(valueExtractor.getValue(any(), eq(SpanInfoFields.ACTION_NAME), any(), any(), any())).thenReturn(originalAction);
        when(replacer.match(any(), any(), eq(SpanInfoFields.ACTION_NAME), eq(originalAction))).thenReturn(null);

        String actionName = spanInfoBuilder.getActionName(mockRule, spanMap, scopeMap, resourceAttributes);

        assertEquals(originalAction, actionName);
    }

    @Test
    public void getTargetName_whenReplacerReturnsValue_returnsReplacedValue() {
        String originalTarget = "originalTarget";
        String replacedTarget = "replacedTarget";
        when(valueExtractor.getValue(any(), eq(SpanInfoFields.TARGET_NAME), any(), any(), any())).thenReturn(originalTarget);
        when(replacer.match(any(), any(), eq(SpanInfoFields.TARGET_NAME), eq(originalTarget))).thenReturn(replacedTarget);

        String targetName = spanInfoBuilder.getTargetName(mockRule, spanMap, scopeMap, resourceAttributes);

        assertEquals(replacedTarget, targetName);
    }

    @Test
    public void getTargetName_whenReplacerReturnsNull_returnsOriginalValue() {
        String originalTarget = "originalTarget";
        when(valueExtractor.getValue(any(), eq(SpanInfoFields.TARGET_NAME), any(), any(), any())).thenReturn(originalTarget);
        when(replacer.match(any(), any(), eq(SpanInfoFields.TARGET_NAME), eq(originalTarget))).thenReturn(null);

        String targetName = spanInfoBuilder.getTargetName(mockRule, spanMap, scopeMap, resourceAttributes);

        assertEquals(originalTarget, targetName);
    }

    @Test
    public void getTargetName_shouldRemoveNullCharacters_whenValueContainsNullBytes() {
        // Arrange
        String originalTarget = "select * from table\u0000";
        String expectedTarget = "select * from table";

        when(valueExtractor.getValue(any(), eq(SpanInfoFields.TARGET_NAME), any(), any(), any())).thenReturn(originalTarget);
        // Assume replacer doesn't change it, or returns null
        when(replacer.match(any(), any(), eq(SpanInfoFields.TARGET_NAME), eq(originalTarget))).thenReturn(null);

        // Act
        String targetName = spanInfoBuilder.getTargetName(mockRule, spanMap, scopeMap, resourceAttributes);

        // Assert
        assertEquals(expectedTarget, targetName, "Should remove null bytes from the target name");
    }
}
