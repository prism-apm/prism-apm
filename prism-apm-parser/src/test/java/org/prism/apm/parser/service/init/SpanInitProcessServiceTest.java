package org.prism.apm.parser.service.init;

import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.config.JacksonConfig;
import org.prism.apm.parser.config.ServiceParseConfig;
import org.prism.apm.parser.delay.DelayService;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.service.common.JacksonJsonService;
import org.prism.apm.parser.service.common.JsonService;
import org.prism.apm.parser.service.parser.ISpanParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpanInitProcessServiceTest {

    @InjectMocks
    private SpanInitProcessService spanInitProcessService;

    @Mock
    private ISpanParser parser;

    @Mock
    private CacheService cacheService;

    @Mock
    private DelayService delayService;

    @Mock
    private ServiceParseConfig serviceParseConfig;

    @BeforeEach
    public void setUp() {
        // Enable the service parse check logic for tests.
        JacksonConfig jacksonConfig = new JacksonConfig();
        JsonService jsonService = new JacksonJsonService(jacksonConfig.objectMapper());

        spanInitProcessService.setJsonService(jsonService);
        spanInitProcessService.setServiceParseCheck(true);

    }

    @Test
    public void testHandleSpan_withValidSpan() {
        // Arrange
        when(serviceParseConfig.needParsed(anyString())).thenReturn(true);
        List<String> spanList = Arrays.asList("spanStr1", "spanStr2");
        String type = "some_type";

        SpanInfo spanInfo1 = mock(SpanInfo.class);
        when(spanInfo1.getSpanId()).thenReturn("spanId1");
        when(spanInfo1.getServiceName()).thenReturn("serviceA");

        SpanInfo spanInfo2 = mock(SpanInfo.class);
        when(spanInfo2.getSpanId()).thenReturn("spanId2");
        when(spanInfo2.getServiceName()).thenReturn("serviceB");

        when(parser.parse("spanStr1")).thenReturn(Collections.singletonList(spanInfo1));
        when(parser.parse("spanStr2")).thenReturn(Collections.singletonList(spanInfo2));


        // Act
        spanInitProcessService.handleSpan(spanList, type);

        // Assert
        verify(cacheService, times(1)).set(eq("spanId1"), anyString());
        verify(cacheService, times(1)).set(eq("spanId2"), anyString());
        verify(delayService, times(1)).processSpan(eq("spanId1"), eq(type));
        verify(delayService, times(1)).processSpan(eq("spanId2"), eq(type));
    }

    @Test
    public void testHandleSpan_withNullOrEmptyParseResult() {
        // Arrange
        List<String> spanList = Collections.singletonList("spanStr1");
        String type = "some_type";

        // Scenario 1: Parser returns null
        when(parser.parse("spanStr1")).thenReturn(null);

        // Act
        spanInitProcessService.handleSpan(spanList, type);

        // Assert
        verify(cacheService, never()).set(anyString(), anyString());
        verify(delayService, never()).processSpan(anyString(), anyString());

        // Scenario 2: Parser returns an empty list
        when(parser.parse("spanStr1")).thenReturn(new ArrayList<>());

        // Act
        spanInitProcessService.handleSpan(spanList, type);

        // Assert (verifications are cumulative, so we check for no *new* invocations)
        verify(cacheService, never()).set(anyString(), anyString());
        verify(delayService, never()).processSpan(anyString(), anyString());
    }

    @Test
    public void testHandleSpan_withFilteredService() {
        // Arrange
        when(serviceParseConfig.needParsed(anyString())).thenReturn(true); // Default behavior
        List<String> spanList = Collections.singletonList("spanStr1");
        String type = "some_type";

        SpanInfo spanInfo = mock(SpanInfo.class);
        when(spanInfo.getServiceName()).thenReturn("filtered-service");
        when(parser.parse("spanStr1")).thenReturn(Collections.singletonList(spanInfo));

        // Mock the config to filter out this specific service
        when(serviceParseConfig.needParsed("filtered-service")).thenReturn(false);

        // Act
        spanInitProcessService.handleSpan(spanList, type);

        // Assert
        verify(cacheService, never()).set(anyString(), anyString());
        verify(delayService, never()).processSpan(anyString(), anyString());
    }

    @Test
    public void testHandleSpan_withException() {
        // Arrange
        List<String> spanList = Collections.singletonList("spanStr1");
        String type = "some_type";
        when(parser.parse(anyString())).thenThrow(new RuntimeException("test exception"));

        // Act
        spanInitProcessService.handleSpan(spanList, type);

        // Assert
        verify(cacheService, never()).set(anyString(), anyString());
        verify(delayService, never()).processSpan(anyString(), anyString());
    }
}
