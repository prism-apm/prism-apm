package org.prism.apm.parser.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.prism.apm.parser.model.otel.OtelResource;
import org.prism.apm.parser.model.otel.OtelResourceSpan;
import org.prism.apm.parser.model.otel.OtelScopeSpan;
import org.prism.apm.parser.model.otel.OtelTraceData;
import org.prism.apm.parser.model.span.SpanInfo;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenTelemetrySpanParser
 */
@ExtendWith(MockitoExtension.class)
public class OpenTelemetrySpanParserTest {

    @Mock
    private OpenTelemetryTraceDeserializer deserializer;

    @Mock
    private OpenTelemetrySpanInfoBuilder spanInfoBuilder;

    @InjectMocks
    private OpenTelemetrySpanParser parser;

    private Map<String, Object> resourceAttributes;
    private Map<String, Object> scopeMap;
    private Map<String, Object> spanMap1;
    private Map<String, Object> spanMap2;
    private SpanInfo spanInfo1;
    private SpanInfo spanInfo2;
    private OtelTraceData traceData;

    @BeforeEach
    public void setUp() {
        // Initialize Map-based test data
        resourceAttributes = Maps.newHashMap("service.name", "test-service");
        scopeMap = Maps.newHashMap("name", "io.opentelemetry.test");
        spanMap1 = Maps.newHashMap("traceId", "trace1");
        spanMap1.put("spanId", "span1");
        spanMap2 = Maps.newHashMap("traceId", "trace2");
        spanMap2.put("spanId", "span2");

        OtelScopeSpan scopeSpan = new OtelScopeSpan();
        scopeSpan.setScope(scopeMap);
        scopeSpan.setSpans(Lists.newArrayList(spanMap1, spanMap2));

        OtelResourceSpan resourceSpan = new OtelResourceSpan();
        OtelResource resource = new OtelResource();
        resource.setAttributes(resourceAttributes);
        resourceSpan.setResource(resource);
        resourceSpan.setScopeSpans(Lists.newArrayList(scopeSpan));

        traceData = new OtelTraceData(Lists.newArrayList(resourceSpan));

        spanInfo1 = completeSpanInfo("trace1", "span1");
        spanInfo2 = completeSpanInfo("trace2", "span2");
    }

    private SpanInfo completeSpanInfo(String traceId, String spanId) {
        SpanInfo spanInfo = new SpanInfo();
        spanInfo.setTraceId(traceId);
        spanInfo.setSpanId(spanId);
        spanInfo.setTime(1L);
        spanInfo.setServiceName("test-service");
        spanInfo.setHostName("test-host");
        spanInfo.setActionName("test-action");
        spanInfo.setTargetName("test-target");
        spanInfo.setRemoteAddr("");
        spanInfo.setSystemType("test-system");
        spanInfo.setComponent("test-component");
        spanInfo.setSpanKind((short) 1);
        spanInfo.setDuration(100L);
        spanInfo.setError(0L);
        return spanInfo;
    }

    /**
     * Tests the successful parsing flow.
     * Verifies that when given a valid Trace JSON with multiple spans, the parser correctly invokes its dependencies and returns a complete list of SpanInfo objects.
     */
    @Test
    public void parse_shouldParseTraceSuccessfullyWhenGivenValidJson() throws JsonProcessingException {
        // Arrange
        String trace = "{\"resourceSpans\":[...]}";

        when(deserializer.deserialize(trace)).thenReturn(traceData);
        when(spanInfoBuilder.build(spanMap1, scopeMap, resourceAttributes)).thenReturn(spanInfo1);
        when(spanInfoBuilder.build(spanMap2, scopeMap, resourceAttributes)).thenReturn(spanInfo2);

        // Act
        List<SpanInfo> result = parser.parse(trace);

        // Assert
        assertNotNull(result, "The result list should not be null.");
        assertEquals(2, result.size(), "The result list should contain 2 elements.");
        assertTrue(result.containsAll(Lists.newArrayList(spanInfo1, spanInfo2)), "The result list should contain all expected SpanInfo objects.");

        verify(deserializer, times(1)).deserialize(trace);
        verify(spanInfoBuilder, times(1)).build(spanMap1, scopeMap, resourceAttributes);
        verify(spanInfoBuilder, times(1)).build(spanMap2, scopeMap, resourceAttributes);
    }

    /**
     * Tests the handling of empty inputs.
     * Verifies that when the input Trace string is `null` or empty, the parser handles it gracefully and returns an empty list.
     */
    @Test
    public void parse_shouldReturnEmptyListWhenInputIsNullOrEmpty() throws JsonProcessingException {
        // Arrange
        when(deserializer.deserialize(null)).thenReturn(null);
        when(deserializer.deserialize("")).thenReturn(null);

        // Act & Assert for null input
        List<SpanInfo> resultForNull = parser.parse(null);
        assertNotNull(resultForNull, "The result for a null input should not be null.");
        assertTrue(resultForNull.isEmpty(), "The result for a null input should be an empty list.");

        // Act & Assert for empty string input
        List<SpanInfo> resultForEmpty = parser.parse("");
        assertNotNull(resultForEmpty, "The result for an empty string input should not be null.");
        assertTrue(resultForEmpty.isEmpty(), "The result for an empty string input should be an empty list.");

        verify(spanInfoBuilder, never()).build(any(), any(), any());
    }

    /**
     * Tests the handling of traces with no spans.
     * Verifies that when the Trace JSON is valid but contains no spans, the parser returns an empty list.
     */
    @Test
    public void parse_shouldReturnEmptyListWhenTraceContainsNoSpans() throws JsonProcessingException {
        // Arrange
        String trace = "{\"resourceSpans\":[]}";
        OtelTraceData emptyTraceData = new OtelTraceData(Collections.emptyList());

        when(deserializer.deserialize(trace)).thenReturn(emptyTraceData);

        // Act
        List<SpanInfo> result = parser.parse(trace);

        // Assert
        assertNotNull(result, "The result should not be null.");
        assertTrue(result.isEmpty(), "The result list should be empty when the trace contains no spans.");

        verify(deserializer, times(1)).deserialize(trace);
        verify(spanInfoBuilder, never()).build(any(), any(), any());
    }

    /**
     * Tests the builder exception handling.
     * Verifies that when the underlying SpanInfoBuilder throws an exception for a span, the parser catches it, continues to process the next span, and logs the error.
     */
    @Test
    public void parse_shouldContinueProcessingWhenBuilderThrowsException() throws JsonProcessingException {
        // Arrange
        String trace = "{\"resourceSpans\":[...]}";
        RuntimeException testException = new RuntimeException("Test Exception from Builder");

        when(deserializer.deserialize(trace)).thenReturn(traceData);
        when(spanInfoBuilder.build(spanMap1, scopeMap, resourceAttributes)).thenThrow(testException);
        when(spanInfoBuilder.build(spanMap2, scopeMap, resourceAttributes)).thenReturn(spanInfo2);

        // Act
        List<SpanInfo> result = parser.parse(trace);

        // Assert
        assertNotNull(result, "The result list should not be null.");
        assertEquals(1, result.size(), "The result list should contain only the successfully built span.");
        assertSame(spanInfo2, result.get(0), "The result list should contain spanInfo2.");

        verify(spanInfoBuilder, times(1)).build(spanMap1, scopeMap, resourceAttributes);
        verify(spanInfoBuilder, times(1)).build(spanMap2, scopeMap, resourceAttributes);
    }

    /**
     * Tests the case where ScopeSpans is null.
     * Verifies that a NullPointerException is thrown as expected when getScopeSpans() returns null.
     */
    @Test
    public void parse_shouldThrowNpeWhenScopeSpansIsNull() throws JsonProcessingException {
        // Arrange
        String trace = "{\"resourceSpans\":[...]}";
        OtelResourceSpan resourceSpan = new OtelResourceSpan();
        resourceSpan.setScopeSpans(null);

        OtelTraceData npeTraceData = new OtelTraceData(Lists.newArrayList(resourceSpan));

        when(deserializer.deserialize(trace)).thenReturn(npeTraceData);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            parser.parse(trace);
        });
    }

    /**
     * Tests that a null SpanInfo from the builder is handled gracefully.
     * Verifies that when the SpanInfoBuilder returns null for one span, the parser skips it, logs a warning, and continues processing other spans.
     */
    @Test
    public void parse_shouldSkipNullSpanAndContinue() throws JsonProcessingException {
        // Arrange
        String trace = "{\"resourceSpans\":[...]}";

        when(deserializer.deserialize(trace)).thenReturn(traceData);
        when(spanInfoBuilder.build(spanMap1, scopeMap, resourceAttributes)).thenReturn(null); // Simulate a build failure for the first span
        when(spanInfoBuilder.build(spanMap2, scopeMap, resourceAttributes)).thenReturn(spanInfo2);

        // Act
        List<SpanInfo> result = parser.parse(trace);

        // Assert
        assertNotNull(result, "The result list should not be null.");
        assertEquals(1, result.size(), "The list should contain only the successfully parsed span.");
        assertTrue(result.contains(spanInfo2), "The list should contain the second span info.");

        verify(spanInfoBuilder, times(1)).build(spanMap1, scopeMap, resourceAttributes);
        verify(spanInfoBuilder, times(1)).build(spanMap2, scopeMap, resourceAttributes);
    }

    /**
     * Tests the handling of JsonProcessingException during deserialization.
     * Verifies that when deserializer.deserialize throws JsonProcessingException, the parser catches it, logs the error, and returns an empty list.
     */
    @Test
    public void parse_shouldReturnEmptyListWhenJsonProcessingExceptionOccurs() throws JsonProcessingException {
        // Arrange
        String invalidTrace = "invalid json";
        when(deserializer.deserialize(invalidTrace)).thenThrow(JsonProcessingException.class);

        // Act
        List<SpanInfo> result = parser.parse(invalidTrace);

        // Assert
        assertNotNull(result, "The result list should not be null.");
        assertTrue(result.isEmpty(), "The result list should be empty when JsonProcessingException occurs.");
    }
}
